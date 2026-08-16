package com.strava.activityservice.importer;

import java.io.InputStream;

public interface ActivityFileParser {

    ParsedActivity parse(InputStream input) throws Exception;

    boolean supports(String fileExtension);
}
