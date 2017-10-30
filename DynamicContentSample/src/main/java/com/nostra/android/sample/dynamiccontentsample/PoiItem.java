package com.nostra.android.sample.dynamiccontentsample;

import android.os.Parcel;
import android.os.Parcelable;

public class PoiItem implements Parcelable {

    private String localName;
    private String localAddress;
    private String localDetail;
    private String telephone;
    private String website;
    private String mediaUrl;
    private double latitude;
    private double longitude;

    public PoiItem() {
    }

    protected PoiItem(Parcel in) {
        localName = in.readString();
        localAddress = in.readString();
        localDetail = in.readString();
        telephone = in.readString();
        website = in.readString();
        mediaUrl = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
    }

    public static final Creator<PoiItem> CREATOR = new Creator<PoiItem>() {
        @Override
        public PoiItem createFromParcel(Parcel in) {
            return new PoiItem(in);
        }

        @Override
        public PoiItem[] newArray(int size) {
            return new PoiItem[size];
        }
    };

    public String getLocalName() {
        return localName;
    }

    public void setLocalName(String localName) {
        this.localName = localName;
    }

    public String getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(String localAddress) {
        this.localAddress = localAddress;
    }

    public String getLocalDetail() {
        return localDetail;
    }

    public void setLocalDetail(String localDetail) {
        this.localDetail = localDetail;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(localName);
        dest.writeString(localAddress);
        dest.writeString(localDetail);
        dest.writeString(telephone);
        dest.writeString(website);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
    }
}
