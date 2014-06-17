package com.google.wallettools;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import java.util.regex.Pattern;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Calendar;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import net.oauth.jsontoken.JsonToken;
import net.oauth.jsontoken.crypto.HmacSHA256Signer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import models.*;

public class JWT_Handler {

    private String ISSUER;
    private String SIGNING_KEY;

    public JWT_Handler(String issuer, String key) {
        this.ISSUER = issuer;
        this.SIGNING_KEY = key;
    }

    public String getJwt(String userData, String fees) throws InvalidKeyException, SignatureException {
        JsonToken token;
        token = createToken(userData, fees);
        return token.serializeAndSign();
    }

    private JsonToken createToken(String userData, String fees) throws InvalidKeyException {
        //Current time and signing algorithm
        Calendar cal = Calendar.getInstance();
        HmacSHA256Signer signer = new HmacSHA256Signer(ISSUER, null, SIGNING_KEY.getBytes());

        //Configure JSON token
        JsonToken token = new net.oauth.jsontoken.JsonToken(signer);
        token.setAudience("Google");
        token.setParam("typ", "google/payments/inapp/item/v1");
        token.setIssuedAt(new org.joda.time.Instant(cal.getTimeInMillis()));
        token.setExpiration(new org.joda.time.Instant(cal.getTimeInMillis() + 120000L));

        //Configure request object, which provides information of the item
        JsonObject request = new JsonObject();
        request.addProperty("name", "Payable Fees for Recent Sales");
        request.addProperty("description", "This payment will bring your account up-to-date.");
        request.addProperty("price", fees);
        request.addProperty("currencyCode", "USD");
        request.addProperty("sellerData", userData);

        JsonObject payload = token.getPayloadAsJsonObject();
        payload.add("request", request);
        return token;
    }

    public String getJwt(Offer offer, Long offerid, Boolean waggle, Boolean highlight) throws InvalidKeyException, SignatureException {
        JsonToken token;
        token = createToken(offer, offerid, waggle, highlight);
        return token.serializeAndSign();
    }

    private JsonToken createToken(Offer offer, Long offerid, Boolean waggle, Boolean highlight) throws InvalidKeyException {
        //Current time and signing algorithm
        Calendar cal = Calendar.getInstance();
        HmacSHA256Signer signer = new HmacSHA256Signer(ISSUER, null, SIGNING_KEY.getBytes());
        float price = 0;
        String description = "Upgrade Options: ";
        if (waggle) {
            price += 1.99;
            description = description + " Waggle Price Label, $1.99 USD";
        }
        if (highlight) {
            if (price > 0) description = description + " + Listing Highlight, $2.99 USD";
            else description = description + " Listing Highlight, $2.99 USD";
            price += 2.99;
        }

        //Configure JSON token
        JsonToken token = new net.oauth.jsontoken.JsonToken(signer);
        token.setAudience("Google");
        token.setParam("typ", "google/payments/inapp/item/v1");
        token.setIssuedAt(new org.joda.time.Instant(cal.getTimeInMillis()));
        token.setExpiration(new org.joda.time.Instant(cal.getTimeInMillis() + 120000L));

        //Configure request object, which provides information of the item
        JsonObject request = new JsonObject();
        request.addProperty("name", "Listing Upgrade - " + offer.title());
        request.addProperty("description", description);
        request.addProperty("price", Float.toString(price));
        request.addProperty("currencyCode", "USD");
        request.addProperty("sellerData", "user_id:"+offer.userid()+",offerid:"+offerid+",waggle:"+waggle+",highlight:"+highlight);

        JsonObject payload = token.getPayloadAsJsonObject();
        payload.add("request", request);

        return token;
    }

    public String getJwt(Offer offer, Long offerid, Long buyerid) throws InvalidKeyException, SignatureException {
        JsonToken token;
        token = createToken(offer, offerid, buyerid);
        return token.serializeAndSign();
    }

    private JsonToken createToken(Offer offer, Long offerid, Long buyerid) throws InvalidKeyException {
        //Current time and signing algorithm
        Calendar cal = Calendar.getInstance();
        HmacSHA256Signer signer = new HmacSHA256Signer(ISSUER, null, SIGNING_KEY.getBytes());

        //Configure JSON token
        JsonToken token = new net.oauth.jsontoken.JsonToken(signer);
        token.setAudience("Google");
        token.setParam("typ", "google/payments/inapp/item/v1");
        token.setIssuedAt(new org.joda.time.Instant(cal.getTimeInMillis()));
        token.setExpiration(new org.joda.time.Instant(cal.getTimeInMillis() + 120000L));

        //Configure request object, which provides information of the item
        JsonObject request = new JsonObject();
        request.addProperty("name", offer.title());
        request.addProperty("description", offer.description());
        request.addProperty("price", Double.toString(offer.price()));
        request.addProperty("currencyCode", offer.currency_code());
        request.addProperty("sellerData", "offerid:"+offerid+",buyerid:"+buyerid);

        JsonObject payload = token.getPayloadAsJsonObject();
        payload.add("request", request);
        return token;
    }

    public String deserialize(String tokenString) {
        String[] pieces = splitTokenString(tokenString);
        String jwtPayloadSegment = pieces[1];
        JsonParser parser = new JsonParser();
        JsonElement payload = parser.parse(StringUtils.newStringUtf8(Base64.decodeBase64(jwtPayloadSegment)));
        return payload.toString();
    }

    /**
     * @param tokenString The original encoded representation of a JWT
     * @return Three components of the JWT as an array of strings
     */
    private String[] splitTokenString(String tokenString) {
        String[] pieces = tokenString.split(Pattern.quote("."));
        if (pieces.length != 3) {
            throw new IllegalStateException("Expected JWT to have 3 segments separated by '"
                    + "." + "', but it has " + pieces.length + " segments");
        }
        return pieces;
    }
}
