package com.example.staffagent.vector;

import java.util.List;

public interface VectorService {
    float[] embed(String text);
    List<float[]> batchEmbed(List<String> texts);
}
