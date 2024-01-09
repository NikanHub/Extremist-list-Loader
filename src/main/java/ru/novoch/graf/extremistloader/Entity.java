package ru.novoch.graf.extremistloader;

import java.time.LocalDate;
import java.util.Arrays;

public class Entity {
	private final static String foreignUL = "Юридическое лицо, включенное в Перечень на основании п.п.6, 7 п.2.1 ст.6 Федерального закона от 07.08.2001 №115-ФЗ";
	private final static String foreignFL = "Физическое лицо, включенное в Перечень на основании п.п.6, 7 п.2.1 ст.6 Федерального закона от 07.08.2001 №115-ФЗ";
	private final static String russianUL = "Юридическое лицо, включенное в Перечень на основании пп.1 - 5 п.2.1 ст.6 Федерального закона от 07.08.2001 № 115-ФЗ";
	private final static String russianFL = "Физическое лицо, включенное в Перечень на основании пп.1 - 5 п.2.1 ст.6 Федерального закона от 07.08.2001 № 115-ФЗ";
	
	private Long ID;
	private String Name;
	private String LastName;
	private String FirstName;
	private String MiddleName;
	private String FmtStr;
	private LocalDate BirthDate;
	private String BirthYear;
	private int iType;
	private String cType;
	private String Inn;
	private String Ogrn;
	private LocalDate DateReg;
	private String BirthPlace;
	private String[] DifferNames;
	
	public Entity() {};
	
	public Long getID() {
		return ID;
	}
	public void setID(Long iD) {
		ID = iD;
	}
	public String getName() {
		return Name;
	}
	public void setName(String name) {
		Name = name;
	}
	public String getLastName() {
		return LastName;
	}
	public void setLastName(String lastName) {
		LastName = lastName;
	}
	public String getFirstName() {
		return FirstName;
	}
	public void setFirstName(String firstName) {
		FirstName = firstName;
	}
	public String getMiddleName() {
		return MiddleName;
	}
	public void setMiddleName(String middleName) {
		MiddleName = middleName;
	}
	public String getFmtStr() {
		return FmtStr;
	}
	public void setFmtStr(String fmtStr) {
		FmtStr = fmtStr;
	}
	public LocalDate getBirthDate() {
		return BirthDate;
	}
	public void setBirthDate(LocalDate birthDate) {
		BirthDate = birthDate;
	}
	public String getBirthYear() {
		return BirthYear;
	}
	public void setBirthYear(String birthYear) {
		BirthYear = birthYear;
	}

	public int getiType() {
		return iType;
	}

	public void setiType(int iType) {
		this.iType = iType;
		switch (iType) {
		case 1: setcType(foreignUL); break;
		case 2: setcType(foreignFL); break;
		case 3: setcType(russianUL); break;
		case 4: setcType(russianFL); break;
		}
	}

	public String getcType() {
		return cType;
	}

	public void setcType(String cType) {
		this.cType = cType;
	}

	public String getInn() {
		return Inn;
	}

	public void setInn(String inn) {
		Inn = inn;
	}

	public String getOgrn() {
		return Ogrn;
	}

	public void setOgrn(String ogrn) {
		Ogrn = ogrn;
	}

	public LocalDate getDateReg() {
		return DateReg;
	}

	public void setDateReg(LocalDate dateReg) {
		DateReg = dateReg;
	}

	public String getBirthPlace() {
		return BirthPlace;
	}

	public void setBirthPlace(String birthPlace) {
		BirthPlace = birthPlace;
	}

	public String[] getDifferNames() {
		return DifferNames;
	}

	public void setDifferNames(String[] differNames) {
		DifferNames = differNames;
	}

	@Override
	public String toString() {
		return "Entity [ID=" + ID + ", Name=" + Name + ", LastName=" + LastName + ", FirstName=" + FirstName
				+ ", MiddleName=" + MiddleName + ", FmtStr=" + FmtStr + ", BirthDate=" + BirthDate + ", BirthYear="
				+ BirthYear + ", iType=" + iType + ", cType=" + cType + ", Inn=" + Inn + ", Ogrn=" + Ogrn + ", DateReg="
				+ DateReg + ", BirthPlace=" + BirthPlace + ", DifferName=" + Arrays.toString(DifferNames) + "]";
	}

}
