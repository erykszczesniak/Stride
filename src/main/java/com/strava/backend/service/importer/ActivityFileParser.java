package com.strava.backend.service.importer;

import java.io.InputStream;

public interface ActivityFileParser {

    ParsedActivity parse(InputStream input) throws Exception;

    boolean supports(String fileExtension);
}
