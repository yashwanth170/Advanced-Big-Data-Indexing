/**
 *
 */
package edu.neu.InsurancePlan.controller;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

import edu.neu.InsurancePlan.config.RabbitMQConfig;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.neu.InsurancePlan.service.JsonSchemaValidatorService;
import edu.neu.InsurancePlan.service.OauthService;
import edu.neu.InsurancePlan.service.PlanService;
//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiOperation;
//import io.swagger.annotations.ApiParam;
import edu.neu.InsurancePlan.utility.ApiError;

import org.springframework.http.HttpHeaders;

/**
 * @author prathmeshc
 */

@RestController
public class PlanController {

    @Autowired
    private JsonSchemaValidatorService jsonSchemaValidatorService;

    @Autowired
    private PlanService planservice;

    @Autowired
    private OauthService oauth;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostMapping(value = "/plan", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> savePlan(@RequestHeader(value = "Authorization", required = false) String bearerToken, @RequestBody String requestBody) {

        if (bearerToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new JSONObject().put("error", "Please pass the Access Token").toString());
        }

        if (!oauth.verifier(bearerToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new JSONObject().put("error", "Invalid Access Token").toString());
        }

        try {
            // jsonSchemaValidatorService.validate(requestBody);
            JsonNode rootNode = jsonSchemaValidatorService.validateSchema(requestBody);
            if (rootNode.get("objectId") == null) {
                ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.toString(), "Please enter a valid json data", new Timestamp(System.currentTimeMillis()));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
            }

            JsonNode objectIdNode = rootNode.path("objectId");

            String objectId = rootNode.get("objectType").textValue() + "-" + rootNode.get("objectId").textValue();
            ;

            System.out.println(objectId);

            boolean existingPlan = planservice.keyExists(objectId);
            System.out.println(existingPlan);

            if (existingPlan) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(new JSONObject().put("error", "Plan with objectId: " + rootNode.get("objectType").textValue() + " already exists").toString());
            }
            ObjectNode requestJson = new ObjectMapper().readValue(requestBody, ObjectNode.class);
            planservice.saveKeyValuePairs(rootNode);
            String planId = rootNode.get("objectType").textValue() + "-" + rootNode.get("objectId").textValue();
            redisTemplate.opsForValue().set(planId, rootNode.toString());
            String newPlan = planservice.fetchObject(planId);
            String etag = MD5(newPlan);

            //Push the message to the queue
            Map<String, String> message = new HashMap<>();
            message.put("operation", "SAVE");
            message.put("body", requestBody);
            rabbitTemplate.convertAndSend(RabbitMQConfig.queueName, message);


            return ResponseEntity.status(HttpStatus.CREATED).eTag(etag).body(new JSONObject().put("message", "Plan added successfully!").toString());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONObject().put("error", e.getMessage()).toString());
        }

    }

    @GetMapping(value = "/plan/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPlan(@PathVariable(value = "id") String id, @RequestHeader(value = "Authorization", required = false) String bearerToken,

                                     @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String eTag) {

        if (bearerToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new JSONObject().put("error", "Please pass the Access Token").toString());
        }

        if (!oauth.verifier(bearerToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new JSONObject().put("error", "Invalid Access Token").toString());
        }

        try {

            String internalId = "plan" + "-" + id;
            String existingPlan = planservice.fetchObject(internalId);
            if (existingPlan == null || existingPlan.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONObject().put("error", "Plan with objectId: " + id + " does not exist").toString());
            }

            String value = planservice.fetchObject(internalId);
            JsonNode jsonNode = jsonSchemaValidatorService.validateSchema(value);
            String existingETag = MD5(existingPlan);
            if (eTag != null && eTag.equals(existingETag)) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(eTag).build();
            }
            return ResponseEntity.ok().eTag(existingETag).body(jsonNode);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONObject().put("error", e.getMessage()).toString());

        }

    }

    @DeleteMapping(value = "/plan/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deletePlan(@RequestHeader(value = "Authorization", required = false) String bearerToken, @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String eTag, @PathVariable(value = "id") String id) {

        if (bearerToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new JSONObject().put("error", "Please pass the Access Token").toString());
        }

        if (!oauth.verifier(bearerToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new JSONObject().put("error", "Invalid Access Token").toString());
        }

        try {
            String internalId = "plan" + "-" + id;
            String value = planservice.fetchObject(internalId);
            if (value == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new JSONObject().put("error", "plan not found!").toString());
            }
            String existingETag = MD5(value);
            if (!eTag.equals(existingETag)) {
                return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(new JSONObject().put("error", "the plan was modified!").toString());
            }

            //Push message to queue for deleting
            Map<String, String> message = new HashMap<>();
            message.put("operation", "DELETE");
            message.put("body", value);
            rabbitTemplate.convertAndSend(RabbitMQConfig.queueName, message);

            planservice.deleteObject(id);
            planservice.deleteKeyValuePairs(new ObjectMapper().readValue(value, ObjectNode.class));
            redisTemplate.getConnectionFactory().getConnection().flushAll();
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONObject().put("error", e.getMessage()).toString());
        }

    }


    @PatchMapping(value = "/plan/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updatePlan(@RequestHeader(value = "Authorization", required = false) String bearerToken, @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String eTag, @RequestBody String requestBody, @PathVariable(value = "id") String id) throws Exception {

        if (bearerToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new JSONObject().put("error", "Please pass the Access Token").toString());
        }

        if (!oauth.verifier(bearerToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new JSONObject().put("error", "Invalid Access Token").toString());
        }

        if (eTag == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONObject().put("error", "No ETag Found").toString());
        }
        String internalID = "plan" + "-" + id;
        System.out.println(internalID);
        String value = planservice.fetchObject(internalID);

        if (value == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new JSONObject().put("error", "No Data Found").toString());
        }

        System.out.println(eTag);
        eTag = eTag.replace("\"", "");
        System.out.println(eTag);
        String newEtag = MD5(value);
        String latestEtag = MD5(requestBody);
        System.out.println("PATCH is " + newEtag);
        System.out.println(requestBody);
        if (eTag == null || eTag == "" || !eTag.equals(newEtag)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(new JSONObject().put("error", "the plan was modified!").toString());
        }

        try {
            // Get the old node from redis using the object Id
            JsonNode oldNode = jsonSchemaValidatorService.validateSchema(value);
            // redisService.populateNestedData(oldNode, null);
            value = oldNode.toString();
            // Construct the new node from the input body
            String inputData = requestBody;
            JsonNode newNode = jsonSchemaValidatorService.validateSchema(inputData);
            ArrayNode planServicesNew = (ArrayNode) newNode.get("linkedPlanServices");
            Set<JsonNode> planServicesSet = new HashSet<>();
            Set<String> objectIds = new HashSet<String>();
            planServicesNew.addAll((ArrayNode) oldNode.get("linkedPlanServices"));
            for (JsonNode node : planServicesNew) {
                Iterator<Map.Entry<String, JsonNode>> sitr = node.fields();
                while (sitr.hasNext()) {
                    Map.Entry<String, JsonNode> val = sitr.next();
                    if (val.getKey().equals("objectId")) {
                        if (!objectIds.contains(val.getValue().toString())) {
                            planServicesSet.add(node);
                            objectIds.add(val.getValue().toString());
                        }
                    }
                }
            }
            planServicesNew.removeAll();
            if (!planServicesSet.isEmpty()) planServicesSet.forEach(s -> {
                planServicesNew.add(s);
            });
            redisTemplate.opsForValue().set(internalID, newNode.toString());
            // planservice.deleteKeyValuePairs(oldNode);
            planservice.saveKeyValuePairs(newNode);
            String existingPlan = planservice.fetchObject(internalID);
            latestEtag = MD5(existingPlan);

            //Push message to queue to Patch
            Map<String, String> message = new HashMap<>();
            message.put("operation", "SAVE");
            message.put("body", requestBody);
            rabbitTemplate.convertAndSend(RabbitMQConfig.queueName, message);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONObject().put("error", "Invalid Data!").toString());
        }

        return ResponseEntity.ok().eTag(latestEtag).body(new JSONObject().put("message", "Patched data with key:" + internalID).toString());

    }

    public String MD5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }


}
