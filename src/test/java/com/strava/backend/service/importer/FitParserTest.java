package com.strava.backend.service.importer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FitParser Unit Tests")
class FitParserTest {

    private FitParser parser;

    @BeforeEach
    void setUp() {
        parser = new FitParser();
    }

    // ---------- test fixture helpers ----------

    /**
     * Build a 12-byte FIT header followed by the given data section bytes.
     * headerSize=12, protocolVersion=0x10, profileVersion=0, dataSize=data.length (LITTLE ENDIAN),
     * signature=".FIT". No header CRC.
     */
    private static byte[] buildFitFile(byte[] data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x0C);                       // header size
        out.write(0x10);                       // protocol version
        out.write(0x00);                       // profile version LE low
        out.write(0x00);                       // profile version LE high
        // data size LE
        out.write(data.length & 0xFF);
        out.write((data.length >> 8) & 0xFF);
        out.write((data.length >> 16) & 0xFF);
        out.write((data.length >> 24) & 0xFF);
        out.write('.');
        out.write('F');
        out.write('I');
        out.write('T');
        out.write(data);
        return out.toByteArray();
    }

    /**
     * Build a FIT header with a RAW 4-byte data_size field (so the test can inject
     * whatever endian pattern it needs) plus the data section.
     */
    private static byte[] buildFitFileWithRawDataSize(byte[] dataSizeBytes, byte[] data) throws IOException {
        if (dataSizeBytes.length != 4) {
            throw new IllegalArgumentException("dataSizeBytes must be 4 bytes");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x0C);
        out.write(0x10);
        out.write(0x00);
        out.write(0x00);
        out.write(dataSizeBytes);
        out.write('.');
        out.write('F');
        out.write('I');
        out.write('T');
        out.write(data);
        return out.toByteArray();
    }

    /**
     * Minimal definition message for local type 0 with zero fields.
     * Layout: recordHeader(0x40), reserved(0), architecture(0=LE), globalMsgNum(2 LE), numFields(0)
     * Total length: 6 bytes.
     */
    private static byte[] definitionMessageEmpty(int localType, int globalMsgNum) {
        return new byte[]{
                (byte) (0x40 | (localType & 0x0F)), // def header bit 6
                0x00,                                // reserved
                0x00,                                // architecture = LE
                (byte) (globalMsgNum & 0xFF),
                (byte) ((globalMsgNum >> 8) & 0xFF),
                0x00                                 // num fields
        };
    }

    // ---------- tests ----------

    @Nested
    @DisplayName("Signature Validation Tests")
    class SignatureTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when signature is not .FIT")
        void shouldThrowForInvalidSignature() throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(0x0C);
            out.write(0x10);
            out.write(0x00);
            out.write(0x00);
            out.write(0x00);
            out.write(0x00);
            out.write(0x00);
            out.write(0x00);
            // bogus signature
            out.write('J');
            out.write('U');
            out.write('N');
            out.write('K');

            assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(out.toByteArray())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid FIT file signature");
        }
    }

    @Nested
    @DisplayName("Minimal FIT File Tests")
    class MinimalFitTests {

        @Test
        @DisplayName("Should parse minimal valid FIT file without throwing")
        void shouldParseMinimalValidFile() throws Exception {
            byte[] data = definitionMessageEmpty(0, 0); // global msg 0 = file_id
            byte[] fitFile = buildFitFile(data);

            ParsedActivity result = parser.parse(new ByteArrayInputStream(fitFile));

            assertThat(result).isNotNull();
            assertThat(result.getGpsPoints()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Endianness Bug Fix Tests")
    class EndiannessTests {

        @Test
        @DisplayName("Should read data_size as little-endian (06 00 00 00 = 6, not 100663296)")
        void shouldReadDataSizeAsLittleEndian() throws Exception {
            // Data section: one empty definition message = 6 bytes
            byte[] data = definitionMessageEmpty(0, 0);
            // Raw little-endian encoding of 6 = {0x06, 0x00, 0x00, 0x00}
            byte[] dataSizeLE = new byte[]{0x06, 0x00, 0x00, 0x00};
            byte[] fitFile = buildFitFileWithRawDataSize(dataSizeLE, data);

            // If dataSize is misread as big-endian, 0x06000000 = 100,663,296 and the parser
            // will keep reading past end of stream and blow up on EOFException. With little-endian
            // the parser reads exactly 6 bytes of data and returns cleanly.
            assertThatCode(() -> parser.parse(new ByteArrayInputStream(fitFile)))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Developer Fields Tests")
    class DeveloperFieldsTests {

        @Test
        @DisplayName("Should handle definition messages with developer fields bit set without misaligning stream")
        void shouldHandleDeveloperFieldsDefinition() throws Exception {
            // Definition message with dev fields bit (0x20) set on the record header.
            // numFields=0, numDevFields=0 → one extra byte (the numDevFields count) after the
            // normal definition layout, which the parser must consume.
            // Layout: header(0x60) reserved(0) arch(0) globalMsg(0 LE x2) numFields(0) numDevFields(0)
            // Total: 7 bytes.
            byte[] defWithDev = new byte[]{
                    (byte) 0x60, // bit 6 = def, bit 5 = dev fields
                    0x00,
                    0x00,
                    0x00, 0x00,
                    0x00, // num fields
                    0x00  // num dev fields
            };

            // Follow it with a data message for local type 0 (just the record header byte,
            // no fields). If the parser misaligned the stream after the def message, this
            // byte would be misinterpreted and the parser would break.
            byte[] dataMsg = new byte[]{0x00};

            ByteArrayOutputStream section = new ByteArrayOutputStream();
            section.write(defWithDev);
            section.write(dataMsg);

            byte[] fitFile = buildFitFile(section.toByteArray());

            assertThatCode(() -> parser.parse(new ByteArrayInputStream(fitFile)))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Compressed Timestamp Header Tests")
    class CompressedTimestampTests {

        @Test
        @DisplayName("Should handle compressed timestamp record headers (bit 7 set) without crashing")
        void shouldHandleCompressedTimestampHeaders() throws Exception {
            // First install a definition for local type 0 with zero fields.
            byte[] def = definitionMessageEmpty(0, 20); // global msg 20 = record
            // Compressed timestamp header: bit 7 = 1, bits 5-6 = local type (0), bits 0-4 = offset (0).
            // With bit 7 set (0x80), the parser must route this through the compressed-timestamp branch,
            // treat it as a data message (not a definition), and use local type from bits 5-6.
            byte[] compressedHeader = new byte[]{(byte) 0x80};

            ByteArrayOutputStream section = new ByteArrayOutputStream();
            section.write(def);
            section.write(compressedHeader);

            byte[] fitFile = buildFitFile(section.toByteArray());

            // The old buggy code treated bit 7 as part of a normal header, which would have set
            // isDefinition=false, localType=0x0F, which has no definition registered, causing
            // the parser to warn + break. Either way it shouldn't throw.
            assertThatCode(() -> parser.parse(new ByteArrayInputStream(fitFile)))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("supports() Tests")
    class SupportsTests {

        @Test
        @DisplayName("Should return true for 'fit'")
        void shouldSupportFit() {
            assertThat(parser.supports("fit")).isTrue();
        }

        @Test
        @DisplayName("Should return true for 'FIT' (case-insensitive)")
        void shouldSupportFitCaseInsensitive() {
            assertThat(parser.supports("FIT")).isTrue();
            assertThat(parser.supports("Fit")).isTrue();
        }

        @Test
        @DisplayName("Should return false for 'gpx'")
        void shouldNotSupportGpx() {
            assertThat(parser.supports("gpx")).isFalse();
        }

        @Test
        @DisplayName("Should return false for 'tcx'")
        void shouldNotSupportTcx() {
            assertThat(parser.supports("tcx")).isFalse();
        }
    }
}
