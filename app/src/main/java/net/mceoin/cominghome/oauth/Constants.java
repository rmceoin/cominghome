package net.mceoin.cominghome.oauth;

public class Constants {

    public static final String CLIENT_ID        = "cc3983dc-29ea-4693-becb-ba24bf8bd94e";
    public static final String CLIENT_SECRET    = "SsA05p3p7KEZIrC8ZJTb0pHL9";

	public static final String CONSUMER_KEY 	= CLIENT_ID; //"anonymous";
	public static final String CONSUMER_SECRET 	= CLIENT_SECRET; //"anonymous";

    public static final String AUTHORIZE_URL 	= "https://home.nest.com/login/oauth2?client_id="+CLIENT_ID+"&state=STATE";
    public static final String ACCESS_URL = "https://api.home.nest.com/oauth2/access_token?code=AUTHORIZATION_CODE&client_id="+
            CLIENT_ID+"&client_secret="+CLIENT_SECRET+"&grant_type=authorization_code";
	

	public static final String	OAUTH_CALLBACK_SCHEME	= "x-oauthflow";
	public static final String	OAUTH_CALLBACK_HOST		= "callback";
	public static final String	OAUTH_CALLBACK_URL		= OAUTH_CALLBACK_SCHEME + "://" + OAUTH_CALLBACK_HOST;

}
