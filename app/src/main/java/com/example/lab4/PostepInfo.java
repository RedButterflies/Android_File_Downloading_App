package com.example.lab4;

import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;


public class PostepInfo implements Parcelable {
    public int mPobranychBajtow;
    public int mRozmiar;
    public String mStatus;

    public PostepInfo() {
    }

    protected PostepInfo(Parcel in) {
        mPobranychBajtow = in.readInt();
        mRozmiar = in.readInt();
        mStatus = in.readString();
    }

    public static final Creator<PostepInfo> CREATOR = new Creator<PostepInfo>() {
        @Override
        public PostepInfo createFromParcel(Parcel in) {
            return new PostepInfo(in);
        }

        @Override
        public PostepInfo[] newArray(int size) {
            return new PostepInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mPobranychBajtow);
        dest.writeInt(mRozmiar);
        dest.writeString(mStatus);
    }

    // method to send progress update
  /*  public void sendProgressUpdate(Context context) {
        Intent progressIntent = new Intent(DownloadService.ACTION_PROGRESS_UPDATE);
        progressIntent.putExtra(DownloadService.EXTRA_PROGRESS, this);
        context.sendBroadcast(progressIntent);
    }*/
}
