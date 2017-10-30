package com.nostra.android.sample.searchsample;

import android.os.Parcel;
import android.os.Parcelable;

import th.co.nostrasdk.common.NTPoint;

public class SearchResult implements Parcelable {
    private String localName;
    private String admin1;
    private String admin2;
    private String admin3;
    private String admin4;
    private NTPoint point;

    SearchResult(String localName, String admin1, String admin2, String admin3, String admin4, NTPoint point) {
        this.localName = localName;
        this.admin1 = admin1;
        this.admin2 = admin2;
        this.admin3 = admin3;
        this.admin4 = admin4;
        this.point = point;
    }

    protected SearchResult(Parcel in) {
        localName = in.readString();
        admin1 = in.readString();
        admin2 = in.readString();
        admin3 = in.readString();
        admin4 = in.readString();
        point = in.readParcelable(NTPoint.class.getClassLoader());
    }

    public static final Creator<SearchResult> CREATOR = new Creator<SearchResult>() {
        @Override
        public SearchResult createFromParcel(Parcel in) {
            return new SearchResult(in);
        }

        @Override
        public SearchResult[] newArray(int size) {
            return new SearchResult[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(localName);
        dest.writeString(admin1);
        dest.writeString(admin2);
        dest.writeString(admin3);
        dest.writeString(admin4);
        dest.writeParcelable(point, flags);
    }

    String getLocalName() {
        return localName;
    }

    String getAdmin1() {
        return admin1;
    }

    String getAdmin2() {
        return admin2;
    }

    String getAdmin3() {
        return admin3;
    }

    String getAdmin4() {
        return admin4;
    }

    NTPoint getPoint() {
        return point;
    }
}
