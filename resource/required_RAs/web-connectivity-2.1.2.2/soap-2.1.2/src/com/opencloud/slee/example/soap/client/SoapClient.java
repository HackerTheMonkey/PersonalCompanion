package com.opencloud.slee.example.soap.client;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Read the SOAP envelope file passed as the second parameter, pass it to the SOAP endpoint passed
 * as the first parameter, and print out the SOAP envelope received as a response.
 */
public class SoapClient {

    public static void main(String[] args) throws Exception {

        if (args.length < 2) {
            System.err.println("Usage: java SoapClient <url> <envelope file>");
            System.exit(1);
        }

        String soapUrl = args[0];
        String fileName = args[1];

        // Create the connection
        URL url = new URL(soapUrl);
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();

        // Read the file into a byte array.
        byte[] b = readToByteArray(fileName);

        // Set the HTTP request properties
        httpConnection.setRequestProperty("Content-Length", String.valueOf(b.length));
        httpConnection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        httpConnection.setRequestMethod("POST");
        httpConnection.setDoOutput(true);
        httpConnection.setDoInput(true);

        // Send the XML that was read in to our byte array
        OutputStream out = httpConnection.getOutputStream();
        out.write(b);
        out.close();

        // Read the response and write it to standard out
        BufferedReader in = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));

        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            System.out.println(inputLine);
        }
        in.close();
    }

    public static byte[] readToByteArray(String fileName) throws IOException {
        FileInputStream fin = new FileInputStream(fileName);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (true) {
            int bytesRead = fin.read(buffer);
            if (bytesRead == -1) break;
            bout.write(buffer, 0, bytesRead);
        }
        fin.close();
        return bout.toByteArray();
    }
}
