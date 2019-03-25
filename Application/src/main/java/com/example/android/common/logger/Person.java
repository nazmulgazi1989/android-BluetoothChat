package com.example.android.common.logger;

public class Person {

    String Id;

    String Fname;
    String Surname;
    String DOB;
    String Gender;
    String MobileNo;
    String PostCode;
    String Ethnicity;
    String EduLevel;
    boolean IsPrimaryProfile;
    String Photo;

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public String getFname() {
        return Fname;
    }

    public void setFname(String fname) {
        Fname = fname;
    }

    public String getSurname() {
        return Surname;
    }

    public void setSurname(String surname) {
        Surname = surname;
    }

    public String getDOB() {
        return DOB;
    }

    public void setDOB(String DOB) {
        this.DOB = DOB;
    }

    public String getGender() {
        return Gender;
    }

    public void setGender(String gender) {
        Gender = gender;
    }

    public String getMobileNo() {
        return MobileNo;
    }

    public void setMobileNo(String mobileNo) {
        MobileNo = mobileNo;
    }

    public String getPostCode() {
        return PostCode;
    }

    public void setPostCode(String postCode) {
        PostCode = postCode;
    }

    public String getEthnicity() {
        return Ethnicity;
    }

    public void setEthnicity(String ethnicity) {
        Ethnicity = ethnicity;
    }

    public String getEduLevel() {
        return EduLevel;
    }

    public void setEduLevel(String eduLevel) {
        EduLevel = eduLevel;
    }

    public boolean isPrimaryProfile() {
        return IsPrimaryProfile;
    }

    public void setPrimaryProfile(boolean primaryProfile) {
        IsPrimaryProfile = primaryProfile;
    }

    public String getPhoto() {
        return Photo;
    }

    public void setPhoto(String photo) {
        Photo = photo;
    }
}
