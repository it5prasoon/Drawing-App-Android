package com.matrix.drawing.data;

public class ImageUploadInfo {
    public String imageURL;
    public ImageUploadInfo() {
    }
    public ImageUploadInfo(String url) {
        this.imageURL= url;
    }
    public String getImageURL() {
        return imageURL;
    }

}