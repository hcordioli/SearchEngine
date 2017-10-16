package com.amazonaws.lambda.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
//import java.sql.Date;
//import java.text.DateFormat;
//import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
//import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
//import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.lambda.runtime.Context;
//import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class LambdaFunctionHandler implements RequestHandler<Object, String> {
		
    @Override
    public String handleRequest(Object input, Context context) {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
        String eanResult = "null";       
        ScanRequest scanRequest = new ScanRequest()
            .withTableName("HotelsQuotation");

        ScanResult result = client.scan(scanRequest);
        
        String header = "" +
				"hotelId|\t"+
				"name|\t" +
				"countryCode|\t" +
				"city|\t" +
                "cityAsRequested|\t" +
                "hotelRating|\t" +
                "highRate|\t" +
                "lowRate|\t" +
                "rateCurrencyCode|\t" +
                "numberOfAdults|\t" +
                "numberOfChildren|\t" +
                "@rate|\t" +
                "@baseRate|\t" +
                "@fenced|\t" +
                "nonRefundable|\t" +
                "rateType|\t" +
                "@total|\t" +
                "@grossProfitOnline|\t" +
                "gpShareHotax|\t" +
                "@pkgSavingsAmount|\t" +
                "hotaxPrice|\t" +
                "hotaxTotalCommission|\t" +
                "hotaxTotalCommissionPercent|\t" +
                "hotaxNetCommission|\t" +
                "hotaxNetCommissionPercent|\t" +
                "storeCommission|\t" +
                "storeCommissionPercent|\t" +
                "hotaxEANStatement|\t"
				;
		System.out.println (header);
        for (Map<String, AttributeValue> destination : result.getItems()){
        	// desmembrar destination nos parâmetros
        	String tempDest = destination.toString();
        	
        	context.getLogger().log("Destination: " + tempDest);
        	String[] splitString = tempDest.split("[S|N]:\\s");
			
    		String temp = splitString[1];
    		String[] thisCity = temp.split (",},");
    		
    		temp = splitString[2];
    		String[] thisNumberOfAdults = temp.split (",},");
    		
    		temp = splitString[4];
    		String[] thisCountryCode = temp.split (",},");
    		
    		temp = splitString[5];
    		String[] thisDepartureDate = temp.split (",},");
    		
    		temp = splitString[6];
    		String[] thisNumberOfResults = temp.split (",},");

    		temp = splitString[7];
    		String[] thisArrivalDate = temp.split (",}");
    		    			
            eanResult = getQuotation(thisCity[0],
									 thisCountryCode[0],
									 thisArrivalDate[0],
									 thisDepartureDate[0],
									 thisNumberOfAdults[0],
									 thisNumberOfResults[0]);
            
            storeResult (thisCity[0], eanResult);
//            System.out.println ("Resposta: "+quotation);
        }
        // TODO: implement your handler
        return ("Hello from Granola V 12: "+eanResult);
    }

    private String getQuotation (String city, String countryCode, String arrivalDate, String departureDate, String numberOfAdults, String numberOfResults) {
    	String cleanCity = city.replaceAll(" ", "%20");
    	long timeInSeconds = (System.currentTimeMillis() / 1000);
		String apikey = "2f46arjpmrl2v4qvhuddsue2uu";
		String secret = "amv3nfo7b0mt9";
		String cid = "491982";
		String accessParms = apikey + secret + timeInSeconds;
		String sigTemp;
		String finalResult = "null";
/*		
		System.out.println (
				"| cid: "+cid+
				"| apikey: "+apikey+
				"| secret: "+secret+
				"| city: "+cleanCity+
				"| countryCode: "+countryCode+
				"| arrivalDate: "+arrivalDate+
				"| departureDate: "+departureDate+
				"| numberOfAdults: "+numberOfAdults+
				"| numberOfResults: "+numberOfResults
				);
*/
		MessageDigest mdx;
		try {
			mdx = MessageDigest.getInstance("MD5");
			mdx.update(accessParms.getBytes());
			sigTemp = String.format("%032x", new BigInteger (1, mdx.digest()));
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			sigTemp = "";
		}
		String sig = sigTemp;
		
		// montar URL Hotel List
		String urlString = "https://book.api.ean.com/ean-services/rs/hotel/v3/list?" + 
				"cid="+ cid +"&" + 
				"minorRev=99" + "&"+
				"apiKey=" + apikey + "&" + 
				"locale=en_US&" + 
				"currencyCode=USD&" + 
				"sig=" + sig + "&" +
				"xml=<HotelListRequest>" + 
				"<city>"+cleanCity+"</city>"+
				"<stateProvinceCode></stateProvinceCode>" + 
				"<countryCode>"+countryCode+"</countryCode>" + 
				"<arrivalDate>"+arrivalDate+"</arrivalDate>" + 
				"<departureDate>"+departureDate+"</departureDate>" + 
				"<RoomGroup>" + 
				"<Room>" + 
				"<numberOfAdults>"+numberOfAdults+"</numberOfAdults>" +
				"</Room>" + 
				"</RoomGroup>" + 
				"<numberOfResults>" + numberOfResults +	"</numberOfResults>" +
				"<rateType>MerchantPackage</rateType>"+
				"</HotelListRequest>";
		
		// invoke EAN API
		URL url;
		try {
			// Read EAN API response
			url = new URL (urlString);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("GET");
			BufferedReader in = new BufferedReader (new InputStreamReader(conn.getInputStream()));
			StringBuffer sb = new StringBuffer();
			sb.append(in.readLine());
			finalResult = sb.toString();
			
//			System.out.println("URL: "+urlString);
//			System.out.println("EAN response:"+sb);
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return (finalResult);
      }
    
    // Salva resultado na tabela DynamoDB HotelsResult
    private Boolean storeResult (String cityAsRequested, String eanResult) {
    	AmazonDynamoDB clientWR = AmazonDynamoDBClientBuilder.standard().build();
        DynamoDB dynamoDB = new DynamoDB(clientWR);
        Table table = dynamoDB.getTable("HotelsResults");
    	
        try {
//        	System.out.println("Adding a new item: "+eanResult);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd");
            LocalDate localDate = LocalDate.now();
            JsonNode rootNode = new ObjectMapper().readTree(new StringReader(eanResult));
    		JsonNode hotelListResponse = rootNode.get("HotelListResponse");
    		JsonNode hotelList = hotelListResponse.get("HotelList");
    		JsonNode hotelSummary = hotelList.get("HotelSummary");

    		String text = hotelList.get("@size").getTextValue();
    		int indexMax = Integer.parseInt (text);
//    		System.out.println("tamanho int:"+indexMax+"       tamanho str:"+text);
    		for(int index=0; index < indexMax; index++){
    			double  pkgSavingsAmount = 0;
//    			System.out.println("      HotelSummary:"+hotelList.get("HotelSummary").get(index));
     			
//    			System.out.println("         hotelId:"+hotelList.get("HotelSummary").get(index).get("hotelId") + "data= "+dtf.format(localDate));
    			String primaryKey = hotelList.get("HotelSummary").get(index).get("hotelId") + "-" + (dtf.format(localDate));
//    			System.out.println("primaryKey= |"+primaryKey+"|");
    			
//    			System.out.println("         name:"+hotelList.get("HotelSummary").get(index).get("name"));
    			String name = hotelList.get("HotelSummary").get(index).get("name").getTextValue();
    			
//    			System.out.println("         city:"+hotelList.get("HotelSummary").get(index).get("city"));
    			String city = hotelList.get("HotelSummary").get(index).get("city").getTextValue();
    			
//    			System.out.println("         countryCode:"+hotelList.get("HotelSummary").get(index).get("countryCode"));
    			String countryCode = hotelList.get("HotelSummary").get(index).get("countryCode").getTextValue();
    			
//    			System.out.println("         hotelRating:"+hotelList.get("HotelSummary").get(index).get("hotelRating"));
    			String hotelRating = hotelList.get("HotelSummary").get(index).get("hotelRating").getValueAsText();
   			
//    			System.out.println("         highRate:"+hotelList.get("HotelSummary").get(index).get("highRate"));
    			String highRateStr = hotelList.get("HotelSummary").get(index).get("highRate").getValueAsText();
    			double  highRate = Float.parseFloat(highRateStr);
    			
//    			System.out.println("         lowRate:"+hotelList.get("HotelSummary").get(index).get("lowRate"));
    			String lowRateStr = hotelList.get("HotelSummary").get(index).get("lowRate").getValueAsText();
    			double  lowRate = Float.parseFloat(lowRateStr);
  					
//    			System.out.println("         rateCurrencyCode:"+hotelList.get("HotelSummary").get(index).get("rateCurrencyCode"));
    			String rateCurrencyCode = hotelList.get("HotelSummary").get(index).get("rateCurrencyCode").getTextValue();
    			    		
//    			JsonNode roomRateDetailsList = hotelList.get("HotelSummary").get(index).get("hotelId").get("RoomRateDetailsList");
//    			JsonNode roomRateDetails = hotelList.get("HotelSummary").get(index).get("hotelId").get("RoomRateDetailsList").get("RoomRateDetails");
//    			System.out.println("         RoomRateDetailsList:"+hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList"));
//    			System.out.println("            RoomRateDetails:"+hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails"));
//    			System.out.println("               roomTypeCode:"+hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("roomTypeCode"));
//    			System.out.println("               RateInfos:"+hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos"));
    			
//    			JsonNode rateInfos = hotelList.get("HotelSummary").get(index).get("hotelId").get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos");
//    			System.out.println("                  RateInfo:"+hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo"));
    			String rateInfoStr = hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo").toString();
//    			System.out.println ("===>"+rateInfoStr);
    			if (rateInfoStr.contains("@pkgSavingsAmount")) { 
//    				System.out.println("                     @pkgSavingsAmount:"+hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo").get("@pkgSavingsAmount"));
    				String pkgSavingsAmountStr = hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo").get("@pkgSavingsAmount").getValueAsText();
    				pkgSavingsAmount = Float.parseFloat(pkgSavingsAmountStr);
    			}
    			else {
    				System.out.println("Não tem pkgSavingsAmount !!");
    				pkgSavingsAmount = 0;
    			}
//    			System.out.println("                     RoomGroup:"+hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo").get("RoomGroup"));
//    			System.out.println("                        Room:"+hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo").get("RoomGroup").get("Room"));
//    			System.out.println("                           numberOfAdults:"+hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo").get("RoomGroup").get("Room").get("numberOfAdults"));
    			String numberOfAdultsStr = hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo").get("RoomGroup").get("Room").get("numberOfAdults").getValueAsText();
    			double numberOfAdults = Float.parseFloat (numberOfAdultsStr);
    			
//    			System.out.println("                           numberOfChildren:"+hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo").get("RoomGroup").get("Room").get("numberOfChildren"));
    			String numberOfChildrenStr = hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo").get("RoomGroup").get("Room").get("numberOfChildren").getValueAsText();
    			double numberOfChildren = Float.parseFloat (numberOfChildrenStr); 
    			
//    			System.out.println("                     ChargeableRateInfo:"+hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo").get("ChargeableRateInfo"));
//    			System.out.println("                        @grossProfitOnline:"+hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo").get("ChargeableRateInfo").get("@grossProfitOnline"));
    			String grossProfitOnlineStr = hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo").get("ChargeableRateInfo").get("@grossProfitOnline").getTextValue();
    			double  grossProfitOnline = Float.parseFloat(grossProfitOnlineStr);
    			
//    			System.out.println("                        @total:"+hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo").get("ChargeableRateInfo").get("@total"));
    			String totalStr = hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo").get("ChargeableRateInfo").get("@total").getTextValue();
    			double  total = Float.parseFloat(totalStr);
    			
//    			System.out.println("                        NightlyRatesPerRoom:"+hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo").get("ChargeableRateInfo").get("NightlyRatesPerRoom"));
//    			System.out.println("                           NightlyRate:"+hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo").get("ChargeableRateInfo").get("NightlyRatesPerRoom").get("NightlyRate"));
    			
    			String text2 = hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo").get("ChargeableRateInfo").get("NightlyRatesPerRoom").get("@size").getTextValue();
    			int indexMaxSize = Integer.parseInt (text2);
    			// Obtendo apenas o primeiro valor do array
    			
//    			for(int index2=0; index2 < indexMaxSize; index2++){
//    				System.out.println("                           @baseRate:"+hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo").get("ChargeableRateInfo").get("NightlyRatesPerRoom").get("NightlyRate").get(0).get("@baseRate"));
    				String baseRateStr = hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo").get("ChargeableRateInfo").get("NightlyRatesPerRoom").get("NightlyRate").get(0).get("@baseRate").getTextValue(); 
    				double  baseRate = Float.parseFloat(baseRateStr); 

//    				System.out.println("                           @rate:"+hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo").get("ChargeableRateInfo").get("NightlyRatesPerRoom").get("NightlyRate").get(0).get("@rate"));
    				String rateStr = hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo").get("ChargeableRateInfo").get("NightlyRatesPerRoom").get("NightlyRate").get(0).get("@rate").getTextValue(); 
    				double  rate = Float.parseFloat(rateStr);

//    				System.out.println("                           @fenced:"+hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo").get("ChargeableRateInfo").get("NightlyRatesPerRoom").get("NightlyRate").get(0).get("@fenced"));
    				String fenced = hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo").get("ChargeableRateInfo").get("NightlyRatesPerRoom").get("NightlyRate").get(0).get("@fenced").getTextValue(); 
    				
//    			}
//    			System.out.println("                     nonRefundable:"+hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo").get("nonRefundable"));
    			String nonRefundable = hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo").get("nonRefundable").getValueAsText();
    			
//    			System.out.println("                     rateType:"+hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo").get("rateType"));
    			String rateType = hotelList.get("HotelSummary").get(index).get("RoomRateDetailsList").get("RoomRateDetails").get("RateInfos").get("RateInfo").get("rateType").getTextValue() ;
     			    			
    			double gpShareHotax = grossProfitOnline / 2;
    			
    			double hotaxPrice = pkgSavingsAmount + total;
    			double hotaxTotalCommission = gpShareHotax + pkgSavingsAmount;
    			double hotaxTotalCommissionPercent = hotaxTotalCommission / hotaxPrice;
    			double storeCommissionPercent = 0.14;
    			double hotaxNetCommissionPercent = hotaxTotalCommissionPercent - storeCommissionPercent;
    			double storeCommission = hotaxPrice * storeCommissionPercent;
    			double hotaxNetCommission = hotaxTotalCommission - storeCommission;
    			double hotaxEANStatement = total - gpShareHotax;
    			double grossProfitPercent = grossProfitOnline / total;
    			double gpShareHotaxPercent = grossProfitPercent * 0.5;
    			
/*    			
    			System.out.println("Demais: "+
    					gpShareHotax+" | "+
    					hotaxPrice+" | "+
    					hotaxTotalCommission+" | "+
    					hotaxTotalCommissionPercent+" | "+
    					hotaxNetCommission+" | "+
    					storeCommission);
    			
    			System.out.println("rateCurrencyCode |"+rateCurrencyCode+"|");
    			System.out.println("rateType |"+rateType+"|");
    			System.out.println("hotelRating |"+hotelRating+"|");
    			
    			System.out.println("pkgSavingsAmount |"+pkgSavingsAmount+"|");
    			System.out.println("numberOfAdults |"+numberOfAdults+"|");
    			System.out.println("numberOfChildren |"+numberOfChildren+"|");
    			System.out.println("rate |"+rate+"|");
    			System.out.println("total |"+total+"|");
    			System.out.println("grossProfitOnline |"+grossProfitOnline+"|");
    			System.out.println("nonRefundable |"+nonRefundable+"|");
    			System.out.println("highRate |"+highRate+"|");
    			System.out.println("lowRate |"+lowRate+"|");
    			System.out.println("baseRate |"+baseRate+"|");
    			System.out.println("fenced |"+fenced+"|");
*/    			    			
    			System.out.println (primaryKey + "|\t" +
    					name + "|\t" +
    					countryCode + "|\t" +
    					city + "|\t"+
                        cityAsRequested + "|\t" +
                        hotelRating + "|\t" +
                        highRate + "|\t" +
                        lowRate + "|\t" +
                        rateCurrencyCode + "|\t" +
                        numberOfAdults + "|\t" +
                        numberOfChildren + "|\t" +
                        rate + "|\t" +
                        baseRate + "|\t" +
                        fenced + "|\t" +
                        nonRefundable + "|\t" +
                        rateType + "|\t" +
                        total + "|\t" +
                        grossProfitOnline + "|\t" +
                        gpShareHotax + "|\t" +
                        pkgSavingsAmount + "|\t" +
                        hotaxPrice + "|\t" +
                        hotaxTotalCommission + "|\t" +
                        hotaxTotalCommissionPercent + "|\t" +
                        hotaxNetCommission + "|\t" +
                        hotaxNetCommissionPercent + "|\t" +
                        storeCommission + "|\t" +
                        storeCommissionPercent + "|\t" +
                        hotaxEANStatement + "|\t");
    			
    			Item item = new Item().withPrimaryKey("hotelId", primaryKey)
                		.withString("name", name)
                        .withString("city", city)
                        .withString("cityAsRequested", cityAsRequested)
                        .withString("countryCode", countryCode)
                        .withString("rateCurrencyCode", rateCurrencyCode)
                        .withString("rateType", rateType)
                        .withString("hotelRating", hotelRating)
                        .withNumber("@pkgSavingsAmount", pkgSavingsAmount)
                        .withNumber("numberOfAdults", numberOfAdults)
                        .withNumber("numberOfChildren", numberOfChildren)
                        .withNumber("@rate", rate)
                        .withNumber("@baseRate", baseRate)
                        .withString("@fenced", fenced)
                        .withNumber("@total", total)
                        .withNumber("@grossProfitOnline", grossProfitOnline)
                        .withNumber("gpShareHotax", gpShareHotax)
                        .withNumber("hotaxPrice", hotaxPrice)
                        .withNumber("hotaxTotalCommission", hotaxTotalCommission)
                        .withNumber("hotaxTotalCommissionPercent", hotaxTotalCommissionPercent)
                        .withNumber("hotaxNetCommission", hotaxNetCommission)
                        .withNumber("storeCommission", storeCommission)
                        .withString("nonRefundable", nonRefundable)
                        .withNumber("highRate", highRate)
                        .withNumber("lowRate", lowRate)
                        .withNumber("hotaxNetCommissionPercent", hotaxNetCommissionPercent)
                        .withNumber("storeCommissionPercent", storeCommissionPercent)
                        .withNumber("hotaxEANStatement", hotaxEANStatement)
                        .withNumber("grossProfitPercent", grossProfitPercent)
                        .withNumber("gpShareHotaxPercent", gpShareHotaxPercent)
                        ;
                PutItemOutcome putItemOutcome = table.putItem(item);
                assert putItemOutcome != null;
/*                
                Item item2 = new Item().withPrimaryKey("hotelId", "9999")
                		.withString("name", "Espelunca")
                        .withString("city", "Pindaíba");
                PutItemOutcome putItemOutcome2 = table.putItem(item2);
                assert putItemOutcome2 != null;
*/
    		}
        }
        catch (Exception e) {
            System.err.println("Unable to add item: ");
            System.err.println(e.getMessage());
        }
        
    	return (true);
    }
}
