package com.udacity.catpoint.imageservice.service;

import java.awt.image.BufferedImage;

public interface IImageService {
    public boolean imageContainsCat(BufferedImage image, float confidenceThreshhold);
}
