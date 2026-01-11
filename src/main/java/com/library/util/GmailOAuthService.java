package com.library.util;

import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;

import java.io.File;
import java.io.InputStreamReader;
import java.util.Collections;

public final class GmailOAuthService {

    private static Gmail gmailService;

    private GmailOAuthService() {}

    public static Gmail getGmailService() {
        if (gmailService != null) return gmailService;

        try {
            var httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            var secretsStream =
                    GmailOAuthService.class.getResourceAsStream("/google-client-secret.json");

            if (secretsStream == null) {
                throw new RuntimeException("Missing Google OAuth client secret");
            }

            var clientSecrets = GoogleClientSecrets.load(
                    GsonFactory.getDefaultInstance(),
                    new InputStreamReader(secretsStream)
            );

            var flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport,
                    GsonFactory.getDefaultInstance(),
                    clientSecrets,
                    Collections.singleton(GmailScopes.GMAIL_SEND)
            )
                    .setAccessType("offline")
                    .setDataStoreFactory(new FileDataStoreFactory(new File("tokens")))
                    .build();


            var receiver = new LocalServerReceiver.Builder().setPort(8888).build();

            var credential = new com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp(
                    flow, receiver).authorize("user");

            gmailService = new Gmail.Builder(
                    httpTransport,
                    GsonFactory.getDefaultInstance(),
                    credential
            ).setApplicationName("LibrarySystem").build();

            return gmailService;

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Gmail service", e);
        }
    }
}
