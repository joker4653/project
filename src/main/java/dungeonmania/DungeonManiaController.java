package dungeonmania;

import dungeonmania.exceptions.InvalidActionException;
import dungeonmania.response.models.BattleResponse;
import dungeonmania.response.models.DungeonResponse;
import dungeonmania.response.models.EntityResponse;
import dungeonmania.response.models.ItemResponse;
import dungeonmania.response.models.RoundResponse;
import dungeonmania.util.Direction;
import dungeonmania.util.FileLoader;
import dungeonmania.util.Position;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DungeonManiaController {
    private int tickCount;
    private List<Entity> listOfEntities = new ArrayList<>();
    private List<String> listOfGoals = new ArrayList<>();
    private HashMap<String, String> configMap = new HashMap<>();
    private String dungeonId;
    private String dungeonName;
    private String goals;
    private HashMap<String, Integer> mapOfMinAndMaxValues = new HashMap<>();
    List<Battle> listOfBattles = new ArrayList<>();
    List<String> buildables = new ArrayList<>();

    public int getTickCount() {
        return tickCount;
    }

    public void setTickCount(int tickCount) {
        this.tickCount = tickCount;
    }


    public String getSkin() {
        return "default";
    }

    public String getLocalisation() {
        return "en_US";
    }

    /**
     * /dungeons
     */
    public static List<String> dungeons() {
        return FileLoader.listFileNamesInResourceDirectory("dungeons");
    }

    /**
     * /configs
     */
    public static List<String> configs() {
        return FileLoader.listFileNamesInResourceDirectory("configs");
    }

    /**
     * /game/new
     */
    public DungeonResponse newGame(String dungeonName, String configName) throws IllegalArgumentException {
        setTickCount(0);

        try {
            String dungeonJSONString = FileLoader.loadResourceFile("/dungeons/" + dungeonName + ".json");
            String configJSONString = FileLoader.loadResourceFile("/configs/" + configName + ".json");

            /* Reading Config file */
            JsonObject configJsonObj = JsonParser.parseString(configJSONString).getAsJsonObject();
            Set<String> configKeySet = configJsonObj.keySet();

            for (String key : configKeySet) {
                configMap.put(key, configJsonObj.get(key).toString());
            }

            /* Reading Dungeon JSON file */
            JsonObject dungeonJsonObj = JsonParser.parseString(dungeonJSONString).getAsJsonObject();

            JsonArray jsonEntities = dungeonJsonObj.get("entities").getAsJsonArray();
            List<EntityResponse> listOfEntityResponses = new ArrayList<>(); 
            for (JsonElement currElement : jsonEntities) {
                JsonObject jsonObjElement = currElement.getAsJsonObject();
                String type = jsonObjElement.get("type").getAsString();
                int x = jsonObjElement.get("x").getAsInt();
                int y = jsonObjElement.get("y").getAsInt();
                
                // fixing key issue
                int key = Integer.MAX_VALUE;
                if (jsonObjElement.get("key") != null) {
                    key = jsonObjElement.get("key").getAsInt();
                }

                Entity entityCreated = createEntity(type, x, y, key);
                if (entityCreated != null) {
                    listOfEntities.add(entityCreated);
                    listOfEntityResponses.add(new EntityResponse(entityCreated.getEntityID(), entityCreated.getEntityType(), entityCreated.getCurrentLocation(), entityCreated.isInteractable()));
                } else {
                    listOfEntityResponses.add(new EntityResponse(UUID.randomUUID().toString(), type, new Position(x, y), false));
                }
            }

            // TODO!!!!! Holly already added the simple goal, BUT NOT the complex goals!!!!!!!!!!!!!!!!!!!!!!!!!!
            JsonElement jsonGoal = dungeonJsonObj.get("goal-condition");
            JsonObject jsonObj = jsonGoal.getAsJsonObject();
            goals = jsonObj.get("goal").getAsString();

            // TODO!!!!! replace the "null" inventory, battles and buildables with your lists.
            this.dungeonId = UUID.randomUUID().toString();
            this.dungeonName = dungeonName;
            DungeonResponse dungeonResp = new DungeonResponse(UUID.randomUUID().toString(), dungeonName, listOfEntityResponses, getInventoryResponse(), getBattleResponse(), buildables, goals);
            
            mapOfMinAndMaxValues = findMinAndMaxValues();

            return dungeonResp;
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    private List<BattleResponse> getBattleResponse() {
        List<BattleResponse> battleRespList = new ArrayList<>();
        Player player = getPlayer();
        Position playerPos = player.getCurrentLocation();

        for (Entity currEntity : listOfEntities) {
            if (currEntity.getCurrentLocation().equals(playerPos) && currEntity.isMovingEntity() && !currEntity.getEntityID().equals(player.getEntityID())) {
                listOfBattles.add(new Battle(player, currEntity));
            }
        }

        for (Battle currBattle : listOfBattles) {
            battleRespList.add(new BattleResponse(currBattle.getEnemyType(), getRoundsResponse(), currBattle.getInitPlayerHealth(), currBattle.getInitEnemyHealth()));
        }

        return battleRespList;
    }

    private List<RoundResponse> getRoundsResponse() {
        // TODO !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        List<RoundResponse> roundRespList = new ArrayList<>();

        

        return roundRespList;
    }

    private List<ItemResponse> getInventoryResponse() {
        Player player = getPlayer();
        ArrayList<Entity> inventory = player.getInventory();
        
        List<ItemResponse> invResponse = new ArrayList<ItemResponse>();

        for (Entity entity : inventory) {
            invResponse.add(new ItemResponse(entity.getEntityID(), entity.getEntityType()));
        }

        return invResponse;
    }

    // helper function that creates entities, which will later be stored in the list of entities
    private Entity createEntity(String type, int x, int y, int key) {
        if (type.equalsIgnoreCase("Player")) {
            return new Player(x, y, configMap);
        } else if (type.equalsIgnoreCase("Spider")) {
            return new Spider(x, y, configMap);
        } else if (type.equalsIgnoreCase("Boulder")) {
            return new Boulder(x, y);
        } else if (type.equalsIgnoreCase("Treasure")) {
            return new Treasure(x, y);
        } else if (type.equalsIgnoreCase("zombie_toast_spawner")) {
            return new ZombieToastSpawner(x, y);
        } else if (type.equalsIgnoreCase("wall")) {
            return new Wall(x, y);
        } else if (type.equalsIgnoreCase("door")) {
            return new Door(x, y, key);
        } else if (type.equalsIgnoreCase("zombie_toast")) {
            return new ZombieToast(x, y, configMap);
        } else if (type.equalsIgnoreCase("mercenary")) {
            return new Mercenary(x, y, configMap);
        } else if (type.equalsIgnoreCase("Treasure")) {
            return new Treasure(x, y);
        } else if (type.equalsIgnoreCase("sword")) {
            return new Sword(x, y, Integer.parseInt(configMap.get("sword_durability")), Integer.parseInt(configMap.get("sword_attack")));
        }
        
        // add other entities here

        return null;
    }

    /**
     * /game/dungeonResponseModel
     */
    public DungeonResponse getDungeonResponseModel() {
        return createDungeonResponse();
    }

    /**
     * /game/tick/item
     */
    public DungeonResponse tick(String itemUsedId) throws IllegalArgumentException, InvalidActionException {
        return createDungeonResponse();
    }

    /**
     * /game/tick/movement
     */
    public DungeonResponse tick(Direction movementDirection) {
        setTickCount(getTickCount() + 1);

        // Move player.
        Player player = getPlayer();
        player.setPrevPos(player.getCurrentLocation()); // a bribed mercenary occupies the player's previous position
        player.move(listOfEntities, movementDirection, player); 

        int xSpi = Integer.parseInt(configMap.get("spider_spawn_rate"));
        int xZomb = Integer.parseInt(configMap.get("zombie_spawn_rate"));
        Spider newSpider = null;

        if (xSpi != 0 && getTickCount() % xSpi == 0) {
            newSpider = new Spider(mapOfMinAndMaxValues.get("minX"), mapOfMinAndMaxValues.get("maxX"),
                            mapOfMinAndMaxValues.get("minY"), mapOfMinAndMaxValues.get("maxY"), configMap);
            newSpider.spawn(listOfEntities, player);
        }

        // all existing moving entities must move
        for (Entity currEntity : listOfEntities) {
            if (currEntity.getEntityType() == "player" || (newSpider != null && currEntity.getEntityID().equalsIgnoreCase(newSpider.getEntityID()))) {
                continue;
            }

            if (currEntity.isMovingEntity())
                ((MovingEntity) currEntity).move(listOfEntities, movementDirection, player);
        }

        if (xZomb != 0 && getTickCount() % xZomb == 0) {
            processZombieSpawner();            
        }

        // update listOfEntities and then dungeonResp
        return createDungeonResponse();
    }

    private void processZombieSpawner() {
        List<Entity> originalList = new ArrayList<>(listOfEntities);
        for (Entity currEntity : originalList) {
            if (currEntity.getEntityType().equalsIgnoreCase("zombie_toast_spawner")) {
                ((ZombieToastSpawner)currEntity).spawnZombie(listOfEntities, configMap);
            }
        }
    }

    // Helper function that creates a new DungeonResponse because some entities can change positions. This new information needs to
    // be included in the listOfEntities and DungeonResponse.
    private DungeonResponse createDungeonResponse() {
        List<EntityResponse> entities = new ArrayList<>();
        for (Entity currEntity : listOfEntities) {
            entities.add(new EntityResponse(currEntity.getEntityID(), currEntity.getEntityType(), currEntity.getCurrentLocation(), currEntity.isInteractable()));
        }

        // TODO replace nulls with correct values as battles and buildables are created!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        DungeonResponse dungeonResp = new DungeonResponse(dungeonId, dungeonName, entities, getInventoryResponse(), getBattleResponse(), buildables, goals);
        return dungeonResp;
    }

    private Player getPlayer() {
        for (Entity entity : listOfEntities) {
            if (entity.getEntityType() == "player") {
                Player player = (Player) entity;
                return player;
            }
        }
        return null;
    }

    private Entity getEntity(String id) {
        for (Entity entity : listOfEntities) {
            if (entity.getEntityID() == id) {
                return entity;
            }
        }
        return null;
    }


    // finds minX, maxX, minY and maxY based on the Dungeon map's coordinates.
    public HashMap<String, Integer> findMinAndMaxValues() {
        List<Integer> listOfXPositions = listOfEntities.stream()
                                                       .map(e -> e.getCurrentLocation().getX())
                                                       .collect(Collectors.toList());

        List<Integer> listOfYPositions = listOfEntities.stream()
                                                       .map(e -> e.getCurrentLocation().getY())
                                                       .collect(Collectors.toList());

        mapOfMinAndMaxValues.put("minX", Collections.min(listOfXPositions));
        mapOfMinAndMaxValues.put("maxX", Collections.max(listOfXPositions));
        mapOfMinAndMaxValues.put("minY", Collections.min(listOfYPositions));
        mapOfMinAndMaxValues.put("maxY", Collections.max(listOfYPositions));

        return mapOfMinAndMaxValues;
    }

    /**
     * /game/build
     */
    public DungeonResponse build(String buildable) throws IllegalArgumentException, InvalidActionException {
        return createDungeonResponse();
    }

    /**
     * /game/interact
     */
    public DungeonResponse interact(String entityId) throws IllegalArgumentException, InvalidActionException {
        Player player = getPlayer();
        // Get the entity.
        Entity entity = getEntity(entityId);
        if (entity == null) {
            throw new IllegalArgumentException("EntityId does not refer to a valid entity.");
        }
        Mercenary merc = (Mercenary) entity;

        // Check player is within radius of mercenary.
        int radius = Integer.parseInt(configMap.get("bribe_radius"));
        if (getDistance(player.getCurrentLocation(), merc.getCurrentLocation()) > radius) {
            throw new InvalidActionException("Mercenary is too far away to bribe.");
        }

        // Check player has sufficient gold - if so, deduct the right amount of gold from player.
        ArrayList<Entity> inventory = player.getInventory();
        List<Entity> treasure = inventory.stream().filter(e -> e.getEntityType().equals("treasure")).collect(Collectors.toList());

        int bribe = Integer.parseInt(configMap.get("bribe_amount"));
        if (treasure.size() < bribe) {
            throw new InvalidActionException("Player lacks the requisite funds to bribe.");
        }

        // Remove gold from inventory.
        for (int i = 0; i < bribe; i++) {
            player.removeItem(treasure.get(i));
        } 

        // Make mercenary into ally.
        merc.setAlly(true);
        merc.setInteractable(false); // according to the spec

        return createDungeonResponse();
    }

    /*
     * @returns int distance, indicating the distance between the two x coordinates, or y
     * coordinates, depending on which is larger.
     */
    private int getDistance(Position a, Position b) {
        int x_diff = Math.abs(a.getX() - b.getX());
        int y_diff = Math.abs(a.getY() - b.getY());
        if (x_diff > y_diff) {
            return x_diff;
        } else {
            return y_diff;
        }
    }
}
