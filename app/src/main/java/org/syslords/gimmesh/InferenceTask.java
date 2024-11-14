/*
 * Copyright (c) 2016-2018 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package org.syslords.gimmesh;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.SystemClock;

import com.qualcomm.qti.snpe.FloatTensor;
import com.qualcomm.qti.snpe.NeuralNetwork;
import com.qualcomm.qti.snpe.Tensor;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InferenceTask extends AsyncTask<Bitmap, Void, Float[][]> {

    private static final int FLOAT_SIZE = 4;

    final String mInputLayer;

    final String mOutputLayer;

    final NeuralNetwork mNeuralNetwork;

    final Bitmap mImage;

    ModelController modelController;

    long mJavaExecuteTime = -1;

    FloatTensor tensor;

    public InferenceTask(NeuralNetwork network, Bitmap image, ModelController modelController, FloatTensor tensor) {
        this.modelController = modelController;
        mNeuralNetwork = network;
        mImage = image;

        Set<String> inputNames = mNeuralNetwork.getInputTensorsNames();
        Set<String> outputNames = mNeuralNetwork.getOutputTensorsNames();
        if (inputNames.size() != 1 || outputNames.size() != 1) {
            throw new IllegalStateException("Invalid network input and/or output tensors.");
        } else {
            mInputLayer = inputNames.iterator().next();
            mOutputLayer = outputNames.iterator().next();
        }

        // this.tensor = tensor;
        this.tensor = mNeuralNetwork.createFloatTensor(mNeuralNetwork.getInputTensorsShapes().get(mInputLayer));
    }

    @Override
    protected Float[][] doInBackground(Bitmap... params) {

        int j = 0;

        final int[] dimensions = tensor.getShape();
        float[] rgbBitmapAsFloat = loadRgbBitmapAsFloat(mImage);

        tensor.write(rgbBitmapAsFloat, 0, rgbBitmapAsFloat.length);

        final Map<String, FloatTensor> inputs = new HashMap<>();
        inputs.put(mInputLayer, tensor);

        final long javaExecuteStart = SystemClock.elapsedRealtime();
        final Map<String, FloatTensor> outputs = mNeuralNetwork.execute(inputs);
        final long javaExecuteEnd = SystemClock.elapsedRealtime();
        mJavaExecuteTime = javaExecuteEnd - javaExecuteStart;

        Float[][] coordinates = new Float[31][2];

        for (Map.Entry<String, FloatTensor> output : outputs.entrySet()) {
            if (output.getKey().equals(mOutputLayer)) {
                FloatTensor outputTensor = output.getValue();

                final float[] array = new float[outputTensor.getSize()];
                outputTensor.read(array, 0, array.length);

//                System.out.println(" " + array.length);

                for (int i = 0; i < 31; ++i) {
//                    System.out.println("x: " + array[i * 4] + " y: " + array[i * 4 + 1]);
//                    if (array[i * 4 + 2] > 0.5) {
                    coordinates[i][0] = array[i * 4];
                    coordinates[i][1] = array[i * 4 + 1];
//                    }
//                    else
//                    {
//                        coordinates[i][0] = 0f;
//                        coordinates[i][1] = 0f;
//                    }
                }

//                float max = array[j];
//
//                for (int i = 0;i < array.length;++i)
//                {
//                    System.out.println(array[i]);
//
//                    if (max < array[i])
//                    {
//                        max = array[i];
//                        j = i;
//                    }
//                }

//                Toast.makeText();

//                System.out.println(yogaPoses[j]);

//                for (Pair<Integer, Float> pair : topK(1, array)) {
//                    result.add(mModel.labels[pair.first]);
//                    result.add(String.valueOf(pair.second));
//                }
            }
        }

        releaseTensors(inputs, outputs);

        return coordinates;
    }

    @Override
    protected void onPostExecute(Float[][] coordinates) {
        super.onPostExecute(coordinates);
        modelController.onClassificationResult(coordinates, mJavaExecuteTime);
    }

    @SafeVarargs
    private final void releaseTensors(Map<String, ? extends Tensor>... tensorMaps) {
        for (Map<String, ? extends Tensor> tensorMap : tensorMaps) {
            for (Tensor tensor : tensorMap.values()) {
                tensor.release();
            }
        }
    }

    float[] loadRgbBitmapAsFloat(Bitmap image) {
        final int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getPixels(pixels, 0, image.getWidth(), 0, 0,
                image.getWidth(), image.getHeight());

        final float[] pixelsBatched = new float[pixels.length * 3];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                final int idx = y * image.getWidth() + x;
                final int batchIdx = idx * 3;
                int pixel = pixels[idx];

                float grayscale = ((pixel >> 16) & 0xFF);
                pixelsBatched[batchIdx] = grayscale;
                grayscale = ((pixel >> 8) & 0xFF);
                pixelsBatched[batchIdx + 1] = grayscale;
                grayscale = (pixel & 0xFF);
                pixelsBatched[batchIdx + 2] = grayscale;
            }
        }
        return pixelsBatched;
    }
}