package ru.novoch.graf.extremistloader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;  
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * 
 * @author Nikan
 * v 1.0
 * Программа позволяет получить данные об экстремистах с указанного URL в формате, описанном в <br>
 * {@link Main#sPattern_russianUL},<br>
 * {@link Main#sPattern_russianFL},<br>
 * {@link Main#sPattern_foreignUL},<br>
 * {@link Main#sPattern_foreignFL},<br>
 * и сформировать xml-файл нужного формата для загрузки перечня в ЦАБС.<br>
 * Чувствительна к формату данных, при любых изменениях требует доработки.<br>
 */

public class Main {
	// для файла настроек
	private final static String cHttp = "HTTP";
	private final static String cFileDir = "FILE_DIR";
	private final static String cLogDir = "LOG_DIR";
	private final static String cSepar = "=";
	private final static String cRem = "#";
	// регулярки для получения данных
	private final static String sPattern_russianUL = "(^\\d*)(.\\s)([\\D]*[^\\d]*)(,\\s)(,\\s)*(ИНН:\\s)*(\\d*)(,\\s)*(ОГРН:\\s)*(\\d*)(,\\s)*(\\d{2}.\\d{2}.\\d{4})*(;)";
	private final static String sPattern_russianFL = "(^\\d*)(.\\s)([\\D]*[^\\d]*)(,\\s)(\\d{2}.\\d{2}.\\d{4})(\\sг.р.\\s,\\s)(\\D*|\\d*)(;)";
	private final static String sPattern_foreignUL = "(^\\d*)(.\\s)([\\D]*[^\\d]*)(,)";
	private final static String sPattern_foreignFL = "(^\\d*)(.\\s)([\\D]*[^\\d]*)(\\d{2}.\\d{2}.\\d{4})*(\\sг.\\sр.,\\s)*([\\D]*[^\\d]*)";
	
	private static String vHttp = "";
	private static String vFileDir = "";
	private static String vLogDir = "";
	
	static org.jsoup.nodes.Document docHtml;
	static org.w3c.dom.Document docXml;
    
    public static boolean isNotNullOrEmpty(String s) {
        return s != null && !s.isEmpty();
    }
    
	private static String checkSlash(String str) {
		String strRez = str;
		if (!"\\".equals(strRez.substring(strRez.length()-1)))
			strRez = strRez.concat("\\\\");
		return strRez;
	}
    
    static Logger LOGGER;

	public static void main(String[] args) {
		
		// чтение настроек из файла %XXI_HOME%/BIN/extremistloader.properties
		String cXXI_HOME = checkSlash(System.getenv("XXI_HOME"));
		try {
			FileReader f_opt = new FileReader(cXXI_HOME.concat("BIN\\extremistloader.properties"));
			Scanner f_scan = new Scanner(f_opt);
			while (f_scan.hasNextLine()) {
				String str = f_scan.nextLine();
				if (!str.substring(0,1).equals(cRem)) {
					if (str.substring(0,str.indexOf(cSepar)).equals(cHttp) ){
						vHttp = str.substring(str.indexOf(cSepar)+cSepar.length());
					}
					else if (str.substring(0,str.indexOf(cSepar)).equals(cFileDir) ){
						vFileDir = checkSlash(str.substring(str.indexOf(cSepar)+cSepar.length()));
					}
					else if (str.substring(0,str.indexOf(cSepar)).equals(cLogDir) ){
						vLogDir = checkSlash(str.substring(str.indexOf(cSepar)+cSepar.length()));
					}
				}
			}
			f_scan.close();
			f_opt.close();
		} catch (IOException e) {
			System.out.println("Error in read file extremistloader.properties: ".concat(e.toString()));
			System.exit(0);
		}
		
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
		LOGGER = Logger.getLogger("ExtrLog"); 
		FileHandler fh;   
		try {
	        String cLogFile = vLogDir.concat("extremistloader.log");
			fh = new FileHandler(cLogFile);
			LOGGER.addHandler(fh);
			SimpleFormatter formatter = new SimpleFormatter(); 
	        fh.setFormatter(formatter);  
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		LOGGER.log(Level.INFO, "Extremist loader begin");
		
		docHtml = null;
		
		List<Entity> arrElements = new ArrayList<Entity>();
		
		try {
			docHtml = Jsoup.connect(vHttp).maxBodySize(Integer.MAX_VALUE).get();
		} catch (IOException e) {
			//System.out.println("Error load http: ".concat(vHttp));
			LOGGER.log(Level.SEVERE, "Error load http: ".concat(vHttp).concat(e.getMessage()));
			System.exit(0);
		}
		
		// Национальные ЮЛ
		Element MainBody = docHtml.getElementById("bodyContent");
		Element NationalBody = MainBody.getElementById("NationalPart");
		Element RussianUL = NationalBody.getElementById("russianUL");
		Elements RussianULs = RussianUL.getElementsByTag("li");
		LOGGER.log(Level.INFO, "russianUL count = ".concat(String.valueOf(RussianULs.size())));
		for (Element aElement : RussianULs) {
			//System.out.println(aElement.text());
			Entity ent = new Entity();
			try {
				Pattern p = Pattern.compile(sPattern_russianUL);
				Matcher m = p.matcher(aElement.text());
				if (m.find()) {
					ent.setID(Long.valueOf(m.group(1)));
					ent.setiType(3);
					String name = m.group(3).replaceAll("[(),]", "").trim();
					if (name.indexOf("*") != -1) {
						ent.setName(name.substring(0,name.indexOf("*")).replaceAll("[(),]", "").trim());
						String differName = name.substring(name.indexOf("*")+1).replaceAll("[(),]", "").trim();
						if (isNotNullOrEmpty(differName)) {
							String[] differNames = differName.split(";");
							for(int i = 0; i < differNames.length; i++) { 
								differNames[i] = differNames[i].trim();  
						      }
							ent.setDifferNames(differNames);
						}
					}
					else
						ent.setName(name.trim());
					ent.setFmtStr(ent.getName());
					ent.setInn(m.group(7));
					ent.setOgrn(m.group(10));
					if (isNotNullOrEmpty(m.group(12))) {
						LocalDate dateReg = LocalDate.parse(m.group(12), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
						ent.setDateReg(dateReg);
					}
				}
				//System.out.println(ent.toString());
			} catch (Exception e) {
				//System.out.println(e.toString());
				LOGGER.log(Level.WARNING, e.toString());
				continue;
			}
			if (ent.getID() != null
					&& isNotNullOrEmpty(ent.getName())
					)
				arrElements.add(ent);
		}
			
		// Национальные ФЛ
		Element RussianFL = NationalBody.getElementById("russianFL");
		Elements RussianFLs = RussianFL.getElementsByTag("li");
		LOGGER.log(Level.INFO, "russianFL count = ".concat(String.valueOf(RussianFLs.size())));
		for (Element aElement : RussianFLs) {
			//System.out.println(aElement.text());
			Entity ent = new Entity();
			try {
				Pattern p = Pattern.compile(sPattern_russianFL);
				Matcher m = p.matcher(aElement.text());
				if (m.find()) {
					ent.setID(Long.valueOf(m.group(1)));
					ent.setiType(4);
					String name = m.group(3).replaceAll("[(),]", "").trim();
					if (name.indexOf("*") != -1) {
						ent.setName(name.substring(0,name.indexOf("*")).replaceAll("[(),]", "").trim());
						String differName = name.substring(name.indexOf("*")+1).replaceAll("[(),]", "").trim();
						if (isNotNullOrEmpty(differName)) {
							String[] differNames = differName.split(";");
							for(int i = 0; i < differNames.length; i++) { 
								differNames[i] = differNames[i].trim();  
						      }
							ent.setDifferNames(differNames);
						}
					}
					else
						ent.setName(name.trim());
					ent.setFmtStr(ent.getName().replace(",", ";").replace("(", "").replace(")", "").replace("*", "").trim());
					if (isNotNullOrEmpty(m.group(5))) {
						LocalDate dateBirth = LocalDate.parse(m.group(5), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
						ent.setBirthDate(dateBirth);
						ent.setBirthYear(ent.getBirthDate().format(DateTimeFormatter.ofPattern("yyyy")));
					}
					ent.setBirthPlace(m.group(7));
				}
				//System.out.println(ent.toString());
			} catch (Exception e) {
				//System.out.println(e.toString());
				LOGGER.log(Level.WARNING, e.toString());
				continue;
			}
			if (ent.getID() != null
					&& isNotNullOrEmpty(ent.getName())
					)
				arrElements.add(ent);
		}
		
	

		Element ForeignUL = docHtml.getElementById("foreignUL");
		Elements ForeignULs = ForeignUL.getElementsByTag("li");
		LOGGER.log(Level.INFO, "foreignUL count = ".concat(String.valueOf(ForeignULs.size())));
		for (Element aElement : ForeignULs) {
			//System.out.println(aElement.text());
			Entity ent = new Entity();
			try {
				Pattern p = Pattern.compile(sPattern_foreignUL);
				Matcher m = p.matcher(aElement.text());
				if (m.find()) {
					ent.setID(Long.valueOf(m.group(1)));
					ent.setiType(1);
					ent.setName(m.group(3).trim());
					String name = m.group(3).replaceAll("[(),]", "").trim();
					if (name.indexOf("*") != -1) {
						ent.setName(name.substring(0,name.indexOf("*")).replaceAll("[(),]", "").trim());
						String differName = name.substring(name.indexOf("*")+1).replaceAll("[(),]", "").trim();
						if (isNotNullOrEmpty(differName)) {
							String[] differNames = differName.split(";");
							for(int i = 0; i < differNames.length; i++) { 
								differNames[i] = differNames[i].trim();  
						      }
							ent.setDifferNames(differNames);
						}
					}
					else
						ent.setName(name);
				}
				//System.out.println(ent.toString());
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, e.toString());
				continue;
			}
			if (ent.getID() != null
					&& isNotNullOrEmpty(ent.getName())
					)
				arrElements.add(ent);
		}
		
		
		
		Element ForeignFL = docHtml.getElementById("foreignFL");
		Elements ForeignFLs = ForeignFL.getElementsByTag("li");
		LOGGER.log(Level.INFO, "foreignFL count = ".concat(String.valueOf(ForeignFLs.size())));
		for (Element aElement : ForeignFLs) {
			//System.out.println(aElement.text());
			Entity ent = new Entity();
			try {
				Pattern p = Pattern.compile(sPattern_foreignFL);
				Matcher m = p.matcher(aElement.text());
				if (m.find()) {
					ent.setID(Long.valueOf(m.group(1)));
					ent.setiType(2);
					String name = m.group(3).replaceAll("[(),]", "").trim();
					if (name.indexOf("*") != -1) {
						ent.setName(name.substring(0,name.indexOf("*")).trim());
						String differName = name.substring(name.indexOf("*")+1).replaceAll("[(),]", "").trim();
						if (isNotNullOrEmpty(differName)) {
							String[] differNames = differName.split(";");
							for(int i = 0; i < differNames.length; i++) { 
								differNames[i] = differNames[i].trim(); 
						      }
							ent.setDifferNames(differNames);
						}
					}
					else
						ent.setName(name);
					ent.setFmtStr(ent.getName().replace(",", ";").replace("(", "").replace(")", "").replace("*", "").trim());
					if (isNotNullOrEmpty(m.group(4))) {
						LocalDate dateBirth = LocalDate.parse(m.group(4), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
						ent.setBirthDate(dateBirth);
						ent.setBirthYear(ent.getBirthDate().format(DateTimeFormatter.ofPattern("yyyy")));
					}
					ent.setBirthPlace(m.group(6));
				}
				//System.out.println(ent.toString());
			} catch (Exception e) {
				//System.out.println(e.toString());
				LOGGER.log(Level.WARNING, e.toString());
				continue;
			}
			if (ent.getID() != null
					&& isNotNullOrEmpty(ent.getName())
					)
				arrElements.add(ent);
		}
		
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
	        docXml = docBuilder.newDocument();
	        //docXml.setXmlStandalone(true);
	        org.w3c.dom.Element SpisokOMU = docXml.createElement("Перечень");
	        SpisokOMU.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xs", "http://www.w3.org/2001/XMLSchema-instance");
	        docXml.appendChild(SpisokOMU);
	        org.w3c.dom.Element VersionFormat = docXml.createElement("ВерсияФормата");
	        VersionFormat.setTextContent("2.0");
	        SpisokOMU.appendChild(VersionFormat);
	        org.w3c.dom.Element DateSpisok = docXml.createElement("ДатаПеречня");
	        DateSpisok.setTextContent(LocalDate.now().toString());
	        SpisokOMU.appendChild(DateSpisok);
	        org.w3c.dom.Element DateLastSpisok = docXml.createElement("ДатаПредыдущегоПеречня");
	        DateLastSpisok.setTextContent(LocalDate.now().minusDays(1L).toString());
	        SpisokOMU.appendChild(DateLastSpisok);
	        org.w3c.dom.Element ActualSpisok = docXml.createElement("АктуальныйПеречень");
	        SpisokOMU.appendChild(ActualSpisok);
			for (Entity aEnt : arrElements) {
				//System.out.println(aEnt.toString());
				org.w3c.dom.Element Subject = docXml.createElement("Субъект");
				ActualSpisok.appendChild(Subject);
				org.w3c.dom.Element SubjectId = docXml.createElement("ИдСубъекта");
				SubjectId.setTextContent(aEnt.getID().toString());
				Subject.appendChild(SubjectId);
				org.w3c.dom.Element SubjectType = docXml.createElement("ТипСубъекта");
				org.w3c.dom.Element SubjectIType = docXml.createElement("Идентификатор");
				String cType = String.valueOf(aEnt.getiType());
				SubjectIType.setTextContent(cType);
				SubjectType.appendChild(SubjectIType);
				org.w3c.dom.Element SubjectCType = docXml.createElement("Наименование");
				SubjectCType.setTextContent(String.valueOf(aEnt.getcType()));
				SubjectType.appendChild(SubjectCType);
				Subject.appendChild(SubjectType);
				switch (aEnt.getiType()) {
				case 1:{
					org.w3c.dom.Element SubjectUL = docXml.createElement("ЮЛ");
					org.w3c.dom.Element SubjectULName = docXml.createElement("Наименование");
					SubjectULName.setTextContent(aEnt.getName());
					SubjectUL.appendChild(SubjectULName);
					if (aEnt.getDifferNames() != null && aEnt.getDifferNames().length > 0) {
						org.w3c.dom.Element SubjectULDifferNames = docXml.createElement("СписокДрНаименований");
						for (String differName : aEnt.getDifferNames()) {
							org.w3c.dom.Element SubjectULDifferName = docXml.createElement("ДрНаименование");
							org.w3c.dom.Element SubjectULDifferNameName = docXml.createElement("Наименование");
							SubjectULDifferNameName.setTextContent(differName);
							SubjectULDifferName.appendChild(SubjectULDifferNameName);
							SubjectULDifferNames.appendChild(SubjectULDifferName);
						}
						SubjectUL.appendChild(SubjectULDifferNames);
					}
					Subject.appendChild(SubjectUL);
					break;}
				case 2:{
					org.w3c.dom.Element SubjectFL = docXml.createElement("ФЛ");
					org.w3c.dom.Element SubjectFIO = docXml.createElement("ФИО");
					SubjectFIO.setTextContent(aEnt.getName());
					SubjectFL.appendChild(SubjectFIO);
					if (aEnt.getBirthDate() != null) {
						org.w3c.dom.Element SubjectBirthDate = docXml.createElement("ДатаРождения");
						SubjectBirthDate.setTextContent(aEnt.getBirthDate().toString());
						SubjectFL.appendChild(SubjectBirthDate);
						org.w3c.dom.Element SubjectBirthYear = docXml.createElement("ГодРождения");
						SubjectBirthYear.setTextContent(aEnt.getBirthYear());
						SubjectFL.appendChild(SubjectBirthYear);
					}
					if (isNotNullOrEmpty(aEnt.getBirthPlace())){
						org.w3c.dom.Element SubjectBirthPlace = docXml.createElement("МестоРождения");
						SubjectBirthPlace.setTextContent(aEnt.getBirthPlace());
						SubjectFL.appendChild(SubjectBirthPlace);
					}
					if (aEnt.getDifferNames() != null && aEnt.getDifferNames().length > 0) {
						org.w3c.dom.Element SubjectFLDifferNames = docXml.createElement("СписокДрНаименований");
						for (String differName : aEnt.getDifferNames()) {
							org.w3c.dom.Element SubjectFLDifferName = docXml.createElement("ДрНаименование");
							org.w3c.dom.Element SubjectULDifferNameFIO = docXml.createElement("ФИО");
							SubjectULDifferNameFIO.setTextContent(differName);
							SubjectFLDifferName.appendChild(SubjectULDifferNameFIO);
							SubjectFLDifferNames.appendChild(SubjectFLDifferName);
						}
						SubjectFL.appendChild(SubjectFLDifferNames);
					}
					Subject.appendChild(SubjectFL);
					break;}
				case 3:{
					org.w3c.dom.Element SubjectUL = docXml.createElement("ЮЛ");
					org.w3c.dom.Element SubjectULName = docXml.createElement("Наименование");
					SubjectULName.setTextContent(aEnt.getName());
					SubjectUL.appendChild(SubjectULName);
					if (aEnt.getDateReg() != null) {
						org.w3c.dom.Element SubjectULDateReg = docXml.createElement("ДатаРегистрации");
						SubjectULDateReg.setTextContent(aEnt.getDateReg().toString());
						SubjectUL.appendChild(SubjectULDateReg);
					}
					if (isNotNullOrEmpty(aEnt.getInn())){
						org.w3c.dom.Element SubjectULInn = docXml.createElement("ИНН");
						SubjectULInn.setTextContent(aEnt.getInn());
						SubjectUL.appendChild(SubjectULInn);
					}
					if (isNotNullOrEmpty(aEnt.getOgrn())){
						org.w3c.dom.Element SubjectULOgrn = docXml.createElement("ОГРН");
						SubjectULOgrn.setTextContent(aEnt.getOgrn());
						SubjectUL.appendChild(SubjectULOgrn);
					}
					if (aEnt.getDifferNames() != null && aEnt.getDifferNames().length > 0) {
						org.w3c.dom.Element SubjectULDifferNames = docXml.createElement("СписокДрНаименований");
						for (String differName : aEnt.getDifferNames()) {
							org.w3c.dom.Element SubjectULDifferName = docXml.createElement("ДрНаименование");
							org.w3c.dom.Element SubjectULDifferNameName = docXml.createElement("Наименование");
							SubjectULDifferNameName.setTextContent(differName);
							SubjectULDifferName.appendChild(SubjectULDifferNameName);
							SubjectULDifferNames.appendChild(SubjectULDifferName);
						}
						SubjectUL.appendChild(SubjectULDifferNames);
					}
					Subject.appendChild(SubjectUL);
					break;}
				case 4:{
					org.w3c.dom.Element SubjectFL = docXml.createElement("ФЛ");
					org.w3c.dom.Element SubjectFIO = docXml.createElement("ФИО");
					SubjectFIO.setTextContent(aEnt.getName());
					SubjectFL.appendChild(SubjectFIO);
					if (aEnt.getBirthDate() != null) {
						org.w3c.dom.Element SubjectBirthDate = docXml.createElement("ДатаРождения");
						SubjectBirthDate.setTextContent(aEnt.getBirthDate().toString());
						SubjectFL.appendChild(SubjectBirthDate);
						org.w3c.dom.Element SubjectBirthYear = docXml.createElement("ГодРождения");
						SubjectBirthYear.setTextContent(aEnt.getBirthYear());
						SubjectFL.appendChild(SubjectBirthYear);
					}
					if (isNotNullOrEmpty(aEnt.getBirthPlace())){
						org.w3c.dom.Element SubjectBirthPlace = docXml.createElement("МестоРождения");
						SubjectBirthPlace.setTextContent(aEnt.getBirthPlace());
						SubjectFL.appendChild(SubjectBirthPlace);
					}
					if (aEnt.getDifferNames() != null && aEnt.getDifferNames().length > 0) {
						org.w3c.dom.Element SubjectFLDifferNames = docXml.createElement("СписокДрНаименований");
						for (String differName : aEnt.getDifferNames()) {
							org.w3c.dom.Element SubjectFLDifferName = docXml.createElement("ДрНаименование");
							org.w3c.dom.Element SubjectULDifferNameFIO = docXml.createElement("ФИО");
							SubjectULDifferNameFIO.setTextContent(differName);
							SubjectFLDifferName.appendChild(SubjectULDifferNameFIO);
							SubjectFLDifferNames.appendChild(SubjectFLDifferName);
						}
						SubjectFL.appendChild(SubjectFLDifferNames);
					}
					Subject.appendChild(SubjectFL);
					break;}
				}
				org.w3c.dom.Element SubjectTerror = docXml.createElement("Террорист");
				SubjectTerror.setTextContent("1");
				Subject.appendChild(SubjectTerror);
			}
        } catch (Exception e) {
     	   //System.out.println("Error in create xml: ".concat(e.toString()).concat(" / "));
        	LOGGER.log(Level.SEVERE, "Error in create xml: ".concat(e.toString()).concat(" / "));
     	   //e.getStackTrace();
     	   System.exit(0);
        }
		
		try {
	        TransformerFactory transformerFactory = TransformerFactory.newInstance();
	        Transformer transformer = transformerFactory.newTransformer();
	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	        DOMSource source = new DOMSource(docXml);
	        String cFile = vFileDir.concat("extremist_".concat(LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")).toString()).concat(".xml"));
	        StreamResult result = new StreamResult(new File(cFile));
	        transformer.transform(source, result);
	        //System.out.println("File saved: ".concat(cFile.replace("\\\\", "\\")));
	        LOGGER.log(Level.INFO, "File saved: ".concat(cFile.replace("\\\\", "\\")));
        } catch (Exception e) {
     	   //System.out.println("Error in save file: ".concat(e.toString()));
        	LOGGER.log(Level.SEVERE, "Error in save file: ".concat(e.toString()));
        	e.getStackTrace();
        }
		
	}

}
