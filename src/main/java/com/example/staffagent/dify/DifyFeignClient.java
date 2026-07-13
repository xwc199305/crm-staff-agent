package com.example.staffagent.dify;

import com.example.staffagent.dify.dto.DifyRequest;
import com.example.staffagent.dify.dto.DifyResponse;
import com.example.staffagent.dify.dto.KnowledgeBaseListResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "dify-client", url = "${dify.base-url:}")
public interface DifyFeignClient {

    @PostMapping(value = "/datasets/{dataset_id}/retrieve", consumes = MediaType.APPLICATION_JSON_VALUE)
    DifyResponse retrieve(
            @PathVariable("dataset_id") String datasetId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody DifyRequest request);

    @GetMapping(value = "/datasets")
    KnowledgeBaseListResponse listDatasets(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("page") int page,
            @RequestParam("limit") int limit);
}