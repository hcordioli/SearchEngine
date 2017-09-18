package searchEngine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;


public class Main {
		public static void main (String[] args ) throws IOException, NoSuchAlgorithmException{

			String fileName = "consultas.csv";
			
			MessageDigest md = MessageDigest.getInstance("MD5");
			long timeInSeconds = (System.currentTimeMillis() / 1000);
			String service = "http://api.ean.com/ean-services/rs/hotel/";
			String version = "v3/";
			String method = "list";
			String hotelId = "201252";
			String apikey = "2f46arjpmrl2v4qvhuddsue2uu";
			String secret = "amv3nfo7b0mt9";
			String cid = "491982";
			String otherElementsStr = "&cid="+cid+"&minorRev=99"+"&locale=en_US&currencyCode=USD";
			String input = apikey + secret + timeInSeconds;
			md.update(input.getBytes());
			String sig = String.format("%032x", new BigInteger(1, md.digest()));
			
			// Cria arquivo de saída
			Path resultsFile = Paths.get("C:/Hotax/Code/results.json");
			BufferedWriter writer = Files.newBufferedWriter(resultsFile, StandardCharsets.UTF_8);
			writer.write("");
			
			File inputFile = new File(fileName);
			try {
				// ler arquivo de parâmetros
				Scanner inputStream = new Scanner (inputFile);
				
				// ignora o cabeçalho
				inputStream.nextLine();
				
				// se existir, trate cada linha
				while (inputStream.hasNext()) {
					String data = inputStream.nextLine();
					String[] values = data.split(",");
					
					String city = values[0];
					String cleanCity = city.replaceAll(" ", "%20");
					System.out.println(city);
					System.out.println(cleanCity);
					
					String countryCode = values[1];
					System.out.println(countryCode);
					
					String arrivalDate = values[2];
					System.out.println(arrivalDate);
					
					String departureDate = values[3]; 
					System.out.println(departureDate);
					
					String numberOfAdults = values[4];
					System.out.println(numberOfAdults);
					
					String numberOfResults = values[5];
					System.out.println(numberOfResults);
					
					// montar URL Hotel ID
//					String urlLink = service + version + method+ "?apikey=" + apikey
//							 + "&sig=" + sig + otherElementsStr + "&hotelIdList=" + hotelId;
//							System.out.println("URL = " + urlLink);
							
					// montar URL Hotel List
					String urlLink = "https://book.api.ean.com/ean-services/rs/hotel/v3/list?" + 
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
					System.out.println("URL = " + urlLink);
					
					// invocar API EAN
					URL url = new URL (urlLink);
					HttpURLConnection conn = (HttpURLConnection)url.openConnection();
					conn.setRequestMethod("GET");
					
					BufferedReader in = new BufferedReader (new InputStreamReader(conn.getInputStream()));
					StringBuffer sb = new StringBuffer();
					String line;
					
					while ((line = in.readLine()) != null) {
						sb.append(line);
						sb.append("\n");
						System.out.println ("Novo registro adicionado: "+line+"\n");
					}
					
					in.close();
					//System.out.println(sb.toString());
					// salvar resposta para LogStash
					System.out.println(sb);
					writer.write(sb.toString());
				}
				inputStream.close();
				writer.close();
				System.out.println ("Fim da consulta.");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
}
