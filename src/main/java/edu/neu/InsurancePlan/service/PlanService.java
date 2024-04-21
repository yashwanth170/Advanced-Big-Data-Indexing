/**
 * 
 */
package edu.neu.InsurancePlan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.data.redis.core.RedisTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.Map;

/**
 * @author prathmeshc
 *
 */

@Service
public class PlanService {

	@Autowired
	private RedisTemplate redisTemplate;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public String saveToRedis(ObjectNode objectNode) throws Exception {
		String eTag;
		if (objectNode.has("objectId")) {
			String key = objectNode.get("objectId").asText();
			String value = objectMapper.writeValueAsString(objectNode);
			redisTemplate.opsForValue().set(key, value);

			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			eTag = bytesToHex(hash);
			//eTag = eTag.length() > 25 ? eTag.substring(0, 25) : eTag;
			//redisTemplate.opsForValue().set(key + ":etag", eTag);

		} else {
			throw new Exception("Missing 'objectId' field in the JSON");
		}

		return eTag;
	}
	
	
	public String fetchEtag(String id) throws Exception {
		String etag = null;
		String data_string = null;
		try {
			data_string  =  (String) redisTemplate.opsForValue().get(id);
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(data_string.getBytes(StandardCharsets.UTF_8));
			etag = bytesToHex(hash);
		} catch (Exception e) {
			// TODO: handle exception
			throw new Exception("No objectid is linked fr given Etag");
		}
		
		return etag;
	}
	
	public String fetchObject(String id) throws Exception {
		String data_string = null;
		
		try {
			 data_string  =  (String) redisTemplate.opsForValue().get(id);
			//data = objectMapper.readValue(data_string, ObjectNode.class);;
			
		} catch (Exception e) {
			// TODO: handle exception
			throw new Exception("No objectid is for given id");
		}
		
		
		
		return data_string;
	}
	
    public boolean keyExists(String key) {
        return redisTemplate.hasKey(key);
    }
    
    
    
    
    public void saveKeyValuePairs(JsonNode rootNode) {
		JsonNode planCostSharesNode = rootNode.get("planCostShares");
		String planCostSharesId = planCostSharesNode.get("objectType").textValue() + "_" + planCostSharesNode.get("objectId").textValue();
		//redisService.postValue(planCostSharesId, planCostSharesNode.toString());
		
		redisTemplate.opsForValue().set(planCostSharesId, planCostSharesNode.toString());
		
		ArrayNode planServices = (ArrayNode) rootNode.get("linkedPlanServices");
		for (JsonNode node : planServices) {
			Iterator<Map.Entry<String, JsonNode>> itr = node.fields();
			if (node != null) {
				redisTemplate.opsForValue().set(node.get("objectType").textValue() + "_" + node.get("objectId").textValue(),
						node.toString());
			}
				

			while (itr.hasNext()) {
				Map.Entry<String, JsonNode> val = itr.next();
				System.out.println(val.getKey());
				System.out.println(val.getValue());
				if (val.getKey().equals("linkedService")) {
					JsonNode linkedServiceNode = (JsonNode) val.getValue();
					redisTemplate.opsForValue().set(linkedServiceNode.get("objectType").textValue() + "_"
							+ linkedServiceNode.get("objectId").textValue(), linkedServiceNode.toString());
				}
				if (val.getKey().equals("planserviceCostShares")) {
					JsonNode planserviceCostSharesNode = (JsonNode) val.getValue();
					redisTemplate.opsForValue().set(
							planserviceCostSharesNode.get("objectType").textValue() + "_"
									+ planserviceCostSharesNode.get("objectId").textValue(),
							planserviceCostSharesNode.toString());
				}
			}
		}
	}
    
    
    
    
    
    public void deleteKeyValuePairs(JsonNode rootNode) {
		JsonNode planCostSharesNode = rootNode.get("planCostShares");
		String planCostSharesId = planCostSharesNode.get("objectType").textValue() + "_" + planCostSharesNode.get("objectId").textValue();
		redisTemplate.delete(planCostSharesId);
		ArrayNode planServices = (ArrayNode) rootNode.get("linkedPlanServices");
		for (JsonNode node : planServices) {
			Iterator<Map.Entry<String, JsonNode>> itr = node.fields();
			if (node != null)
				redisTemplate.delete(node.get("objectType").textValue() + "_" + node.get("objectId").textValue());

			while (itr.hasNext()) {
				Map.Entry<String, JsonNode> val = itr.next();
				System.out.println(val.getKey());
				System.out.println(val.getValue());
				if (val.getKey().equals("linkedService")) {
					JsonNode linkedServiceNode = (JsonNode) val.getValue();
					redisTemplate.delete(linkedServiceNode.get("objectType").textValue() + "_"
							+ linkedServiceNode.get("objectId").textValue());

				}
				if (val.getKey().equals("planserviceCostShares")) {
					JsonNode planserviceCostSharesNode = (JsonNode) val.getValue();
					redisTemplate.delete(planserviceCostSharesNode.get("objectType").textValue() + "_"
							+ planserviceCostSharesNode.get("objectId").textValue());

				}
			}
		}
	}
    
    
    
	
	
	public void deleteObject(String id) throws Exception {
		
		
		try {
			
		    redisTemplate.delete(id);
		    redisTemplate.delete("plan_"+id );
			
		} catch (Exception e) {
			// TODO: handle exception
			throw new Exception("No objectid is for given id");
		}
	}

	private static String bytesToHex(byte[] hash) {
		StringBuilder hexString = new StringBuilder();
		for (byte b : hash) {
			String hex = Integer.toHexString(0xff & b);
			if (hex.length() == 1) {
				hexString.append('0');
			}
			hexString.append(hex);
		}
		return hexString.toString();
	}

}
