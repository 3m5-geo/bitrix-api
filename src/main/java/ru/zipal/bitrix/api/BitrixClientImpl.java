package ru.zipal.bitrix.api;

import org.apache.http.NameValuePair;
import org.json.JSONObject;

import java.util.List;

public class BitrixClientImpl implements BitrixClient {
    public static final String URL_FORMAT = "https://%s/rest/%s.json?auth=%s";
    public static final String TOKEN_URL_FORMAT = "https://%s/oauth/token/?client_id=%s&client_secret=%s&grant_type=refresh_token&refresh_token=%s";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String ACCESS_TOKEN = "access_token";

    private final BitrixHttpClient httpClient;
    private final Tokens tokens;
    private final String appId;
    private final String appSecret;

    public BitrixClientImpl(BitrixHttpClient httpClient, Tokens tokens, String appId, String appSecret) {
        this.httpClient = httpClient;
        this.tokens = tokens;
        this.appId = appId;
        this.appSecret = appSecret;
    }

    private String getUrl(String domain, String method, String accessToken) {
        return String.format(URL_FORMAT, domain, method, accessToken);
    }

    private String getTokenUrl(String domain, String refreshToken) {
        return String.format(TOKEN_URL_FORMAT, domain, appId, appSecret, refreshToken);
    }

    @Override
    public JSONObject execute(String domain, String method, List<NameValuePair> params) throws BitrixApiException {
        try {
            return httpClient.post(getUrl(domain, method, tokens.getAccessToken()), params);
        } catch (UnauthorizedBitrixApiException e) {
            final String newAccessToken = getAccessToken(domain, tokens);
            return httpClient.post(getUrl(domain, method, newAccessToken), params);
        }
    }

    @Override
    public JSONObject execute(String domain, String method, JSONObject params) throws BitrixApiException {
        try {
            return httpClient.post(getUrl(domain, method, tokens.getAccessToken()), params);
        } catch (UnauthorizedBitrixApiException e) {
            final String newAccessToken = getAccessToken(domain, tokens);
            return httpClient.post(getUrl(domain, method, newAccessToken), params);
        }
    }

    private String getAccessToken(String domain, Tokens tokens) throws BitrixApiException {
        final JSONObject json = httpClient.get(getTokenUrl(domain, tokens.getRefreshToken()));
        final String newRefreshToken;
        if (json.has(REFRESH_TOKEN)) {
            newRefreshToken = json.getString(REFRESH_TOKEN);
        } else {
            newRefreshToken = null;
        }
        final String newAccessToken = json.getString(ACCESS_TOKEN);
        tokens.updateTokens(newAccessToken, newRefreshToken);
        return newAccessToken;
    }
}
