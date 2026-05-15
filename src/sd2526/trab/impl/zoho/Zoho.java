package sd2526.trab.impl.zoho;

import java.util.List;
import java.util.Map;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

import sd2526.trab.impl.zoho.msgs.ZohoAccount;
import sd2526.trab.impl.zoho.msgs.ZohoAccountReply;
import sd2526.trab.impl.zoho.msgs.ZohoFolder;
import sd2526.trab.impl.zoho.msgs.ZohoFoldersReply;
import sd2526.trab.impl.zoho.msgs.ZohoMessage;
import sd2526.trab.impl.zoho.msgs.ZohoMessageReply;
import sd2526.trab.impl.zoho.msgs.ZohoMessagesReply;
import sd2526.trab.impl.utils.JSON;

public class Zoho {
    static final String MAIL_API_BASE = "https://mail.zoho.eu/api";

    static final String CLIENT_ID = "1000.FLLFY50XXI3MDNGK5J5LD6C0MWZH0Z";
    static final String CLIENT_SECRET = "b2353340ef9d41595cf22ed06e62b0d0064e3ed1da";
    static final String REFRESH_TOKEN = "1000.fa68bb9e170e3bbe34dcc6ca15d84089.c4618531950aac1c05d2bbbda10f3987";

    private static final String ACCOUNTS = "/accounts";
    private static final String MESSAGES = "/messages";
    private static final String FOLDERS = "/folders";

    private String accountId;
    private String mailboxAddress;
    private String inboxFolderId;

    final OAuth20Service service;
    final ZohoTokenManager tokenManager;

    static Zoho instance;

    private Zoho() {
        service = ZohoServiceFactory.buildService(CLIENT_ID, CLIENT_SECRET);
        tokenManager = new ZohoTokenManager(service, REFRESH_TOKEN);
        try {
            var account = getAccount();
            if (account != null) {
                this.accountId = account.accountId();
                this.mailboxAddress = account.mailboxAddress();
                this.inboxFolderId = getInboxFolderId(); // Está a dar null
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    synchronized public static Zoho getInstance() {
        if (instance == null)
            instance = new Zoho();
        return instance;
    }

    private String getInboxFolderId() throws Exception {
        var accessToken = new OAuth2AccessToken(tokenManager.getValidAccessToken());

        OAuthRequest request = new OAuthRequest(Verb.GET,
                MAIL_API_BASE + ACCOUNTS + "/" + accountId + FOLDERS);
        service.signRequest(accessToken, request);

        try (Response response = service.execute(request)) {
            if (response.isSuccessful()) {
                var folders = JSON.decode(response.getBody(), ZohoFoldersReply.class).data();
                return folders.stream()
                        .filter(f -> "Inbox".equals(f.folderType()))
                        .map(ZohoFolder::folderId)
                        .findFirst()
                        .orElse(null);
            }
            return null;
        }
    }

    public ZohoAccount getAccount() throws Exception {
        var accessToken = new OAuth2AccessToken(tokenManager.getValidAccessToken());

        OAuthRequest request = new OAuthRequest(Verb.GET, MAIL_API_BASE + ACCOUNTS);
        service.signRequest(accessToken, request);

        try (Response response = service.execute(request)) {
            if (response.isSuccessful()) {
                var body = response.getBody();
                var data = JSON.decode(body, ZohoAccountReply.class).data();
                if (data == null || data.isEmpty())
                    return null;
                return data.get(0);
            } else {
                System.err.println(response.getCode() + "/" + response.getBody());
                return null;
            }
        }
    }

    public String sendMessage(ZohoMessage msg) throws Exception {
        var accessToken = new OAuth2AccessToken(tokenManager.getValidAccessToken());

        var payload = Map.of(
                "fromAddress", mailboxAddress,
                "toAddress", mailboxAddress,
                "subject", msg.subject(),
                "content", msg.content());

        OAuthRequest request = new OAuthRequest(Verb.POST, MAIL_API_BASE + ACCOUNTS + "/" + accountId + MESSAGES);
        request.addHeader("Content-Type", "application/json");
        request.setPayload(JSON.encode(payload));
        service.signRequest(accessToken, request);

        try (Response response = service.execute(request)) {
            if (response.isSuccessful()) {
                var body = response.getBody();
                var data = JSON.decode(body, ZohoMessageReply.class).data();
                if (data == null)
                    return null;
                return data.messageId();
            } else {
                System.err.println(response.getCode() + "/" + response.getBody());
                return null;
            }
        }
    }

    public ZohoMessage getMessage(String messageId) throws Exception {
        var accessToken = new OAuth2AccessToken(tokenManager.getValidAccessToken());

        OAuthRequest request = new OAuthRequest(Verb.GET,
                MAIL_API_BASE + ACCOUNTS + "/" + accountId + FOLDERS + "/" + inboxFolderId + MESSAGES + "/" + messageId
                        + "/content");
        service.signRequest(accessToken, request);

        try (Response response = service.execute(request)) {
            if (response.isSuccessful()) {
                var body = response.getBody();
                var data = JSON.decode(body, ZohoMessageReply.class).data();
                if (data == null)
                    return null;
                return data;
            } else {
                System.err.println(response.getCode() + "/" + response.getBody());
                return null;
            }
        }
    }

    public List<String> getMessages(String query) throws Exception {
        var accessToken = new OAuth2AccessToken(tokenManager.getValidAccessToken());

        var url = "";

        if (query == null || query.isEmpty()) {
            url = MAIL_API_BASE + ACCOUNTS + "/" + accountId + MESSAGES + "/view";
        } else {
            url = MAIL_API_BASE + ACCOUNTS + "/" + accountId + MESSAGES + "/search";
        }

        OAuthRequest request = new OAuthRequest(Verb.GET, url);

        if (query != null && !query.isEmpty()) {
            request.addQuerystringParameter("searchKey", query);
        }
        service.signRequest(accessToken, request);

        try (Response response = service.execute(request)) {
            if (response.isSuccessful()) {
                var body = response.getBody();
                var data = JSON.decode(body, ZohoMessagesReply.class).data();
                if (data == null)
                    return List.of();
                return data.stream().map(ZohoMessage::messageId).toList();
            } else {
                System.err.println(response.getCode() + "/" + response.getBody());
                return List.of();
            }
        }
    }

    public void removeInboxMessage(String messageId) throws Exception {
        var accessToken = new OAuth2AccessToken(tokenManager.getValidAccessToken());

        OAuthRequest request = new OAuthRequest(Verb.DELETE,
                MAIL_API_BASE + ACCOUNTS + "/" + accountId + FOLDERS + "/" + inboxFolderId + MESSAGES + "/"
                        + messageId);
        request.addQuerystringParameter("expunge", "true");
        service.signRequest(accessToken, request);

        try (Response response = service.execute(request)) {
            if (!response.isSuccessful()) {
                System.err.println(response.getCode() + "/" + response.getBody());
            }
        }
    }

    public void clearInbox() throws Exception {
        var msgIds = getMessages(null);
        for (String msgId : msgIds) {
            try {
                removeInboxMessage(msgId);
            } catch (Exception e) {
                System.err.println("Error occurred while removing message: " + msgId);
                e.printStackTrace();
            }
        }
    }

}