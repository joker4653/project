package dungeonmania;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dungeonmania.exceptions.InvalidActionException;

import static dungeonmania.TestUtils.getPlayer;
import static dungeonmania.TestUtils.getEntities;
import static dungeonmania.TestUtils.getInventory;
import static dungeonmania.TestUtils.getGoals;
import static dungeonmania.TestUtils.countEntityOfType;
import static dungeonmania.TestUtils.getValueFromConfigFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dungeonmania.response.models.BattleResponse;
import dungeonmania.response.models.DungeonResponse;
import dungeonmania.response.models.EntityResponse;
import dungeonmania.response.models.RoundResponse;
import dungeonmania.util.Direction;
import dungeonmania.util.Position;

public class ZombieTests {
    // Helper function that gets position of a zombie
    public Position getZombPos(int i, DungeonResponse res) {
        return getEntities(res, "zombie_toast").stream()
                                                    .filter(it -> it.getType().equalsIgnoreCase("zombie_toast"))
                                                    .collect(Collectors.toList())
                                                    .get(i)
                                                    .getPosition();
    }

    // Helper function that gets size of the zombie list
    public int getZombSize(DungeonResponse res) {
        return getEntities(res, "zombie_toast").stream()
                                            .filter(it -> it.getType().equalsIgnoreCase("zombie_toast"))
                                            .collect(Collectors.toList())
                                            .size();
    }

    // Helper function that returns a list of zombie spawn locations
    public List<Position> getSpawnLocations(DungeonResponse res, Position spawnerPos) {
        Position left = new Position(spawnerPos.getX() - 1, spawnerPos.getY());
        Position right = new Position(spawnerPos.getX() + 1, spawnerPos.getY());
        Position up = new Position(spawnerPos.getX(), spawnerPos.getY() - 1);
        Position below = new Position(spawnerPos.getX(), spawnerPos.getY() + 1);

        List<Position> possibleZombiePos = Arrays.asList(left, right, up, below);

        return possibleZombiePos;
    }

    // Zombie toast spawner destruction tests.
     /*
     * - [Spawner] Player is not cardinally adjacent to spawner.
     * - [Spawner] Player does not have a weapon for destroying spawner.
     * - [Spawner] Destruction success.
     */
    @Test
    @DisplayName("Test spawner interact when not cardinally adjacent.")
    public void testSpawnerDestructionNotAdjacent() {
        DungeonManiaController dmc = new DungeonManiaController();
        DungeonResponse res = dmc.newGame("d_spawnerDestructionTest_basic", "c_spawnerDestructionTest_basic");

        EntityResponse spawner = getEntities(res, "zombie_toast_spawner").get(0);

        assertThrows(InvalidActionException.class, () -> {
            dmc.interact(spawner.getId());
        });
    }

    @Test
    @DisplayName("Test spawner interact without weapon.")
    public void testSpawnerDestructionNoWeapon() {
        DungeonManiaController dmc = new DungeonManiaController();
        DungeonResponse res = dmc.newGame("d_spawnerDestructionTest_basic", "c_spawnerDestructionTest_basic");

        res = dmc.tick(Direction.RIGHT);

        EntityResponse spawner = getEntities(res, "zombie_toast_spawner").get(0);

        assertThrows(InvalidActionException.class, () -> {
            dmc.interact(spawner.getId());
        });
    }

    @Test
    @DisplayName("Test spawner interact success case.")
    public void testSpawnerDestructionSuccess() {
        DungeonManiaController dmc = new DungeonManiaController();
        DungeonResponse res = dmc.newGame("d_spawnerDestructionTest_sword", "c_spawnerDestructionTest_basic");

        res = dmc.tick(Direction.RIGHT);

        EntityResponse spawner = getEntities(res, "zombie_toast_spawner").get(0);

        assertDoesNotThrow(() -> {
            dmc.interact(spawner.getId());
        });

        res = dmc.tick(Direction.RIGHT);

        assertEquals(0, countEntityOfType(res, "zombie_toast_spawner"));

    }


    // Zombie toast spawn tests.
    @Test
    @DisplayName("Test zombies can only spawn on cardinally adjacent open squares")
    public void testZombiesSpawnSuccess() {
        /* Test zombies spawn on a cardinally adjacent (up, down, left, right) “open square” (i.e. no wall or boulder).
        Also ensure no zombies spawn on top of the spawner (assert that the no. of zombies on the spawner = 0) */

        DungeonManiaController dmc = new DungeonManiaController();
        DungeonResponse res = dmc.newGame("d_zombieTest_spawnSuccess", "c_zombieTest_spawnSuccess");

        Position spawnerPos = getEntities(res, "zombie_toast_spawner").get(0).getPosition();
        List<Position> possibleSpawnPos = getSpawnLocations(res, spawnerPos);
        Position currZombiePos = null;
        int numZombs = 0;
        for (int i = 1; i < 31; i++) {
            res = dmc.tick(Direction.DOWN);

            if (i % 3 == 0) {
                currZombiePos = getZombPos(numZombs, res);
                assertTrue(possibleSpawnPos.contains(currZombiePos));
                assertTrue(currZombiePos != spawnerPos); // zombies can spawn on top of their spawners
                numZombs++;
            }
        }
    }

    @Test
    @DisplayName("Test zombies can't spawn on walls, boulders and locked doors")
    public void testZombiesCantSpawn() {
        DungeonManiaController dmc = new DungeonManiaController();
        DungeonResponse res = dmc.newGame("d_zombieTest_spawnBlocked", "c_zombieTest_spawnBlocked");
        
        for (int i = 0; i < 30; i++) {
            res = dmc.tick(Direction.DOWN);
        }

        assertEquals(getZombSize(res), 0);
    }

    @Test
    @DisplayName("Test zombies spawn when zombie_spawn_rate = 0")
    public void testZombiesCantSpawnWhenRateIs0() {
        DungeonManiaController dmc = new DungeonManiaController();
        DungeonResponse res = dmc.newGame("d_zombieTest_spawnSuccess", "c_spiderTest_spawnEveryTick");
        
        for (int i = 0; i < 30; i++) {
            res = dmc.tick(Direction.DOWN);
        }

        assertEquals(getZombSize(res), 0);
    }

    @Test
    @DisplayName("Test correct no. of zombies spawn when zombie_spawn_rate = 1")
    public void testZombiesSpawnEveryTick() {
        DungeonManiaController dmc = new DungeonManiaController();
        DungeonResponse res = dmc.newGame("d_zombieTest_spawnEveryTick", "c_zombieTest_spawnEveryTick");
        
        for (int i = 0; i < 10; i++) {
            res = dmc.tick(Direction.DOWN);
        }

        assertEquals(getZombSize(res), 10);
    }

    @Test
    @DisplayName("Test correct no. of zombies spawn when zombie_spawn_rate = 10")
    public void testZombiesSpawnEvery10Ticks() {
        DungeonManiaController dmc = new DungeonManiaController();
        DungeonResponse res = dmc.newGame("d_zombieTest_spawnEvery10Ticks", "c_zombieTest_spawnEvery10Ticks");
        
        for (int i = 0; i < 50; i++) {
            res = dmc.tick(Direction.DOWN);
        }

        assertEquals(getZombSize(res), 5);
    }

    @Test
    @DisplayName("Test more zombies can't be spawned without a zombie spawner")
    public void testNoNewZombiesWithoutSpawner() {
        DungeonManiaController dmc = new DungeonManiaController();
        DungeonResponse res = dmc.newGame("d_zombieTest_cantSpawnWithNoSpawner", "c_zombieTest_spawnEveryTick");

        for (int i = 0; i < 50; i++) {
            res = dmc.tick(Direction.LEFT);
        }

        assertEquals(getZombSize(res), 1);
    }

    @Test
    @DisplayName("Test zombies can't be spawned at all")
    public void testNoZombiesAtAll() {
        DungeonManiaController dmc = new DungeonManiaController();
        DungeonResponse res = dmc.newGame("d_zombieTest_cantSpawnAtAll", "c_zombieTest_spawnEveryTick");

        for (int i = 0; i < 3; i++) {
            res = dmc.tick(Direction.UP);
        }
        
        assertEquals(getZombSize(res), 0);
    }

    @Test
    @DisplayName("Test multiple zombies can spawn from many different spawners")
    public void testMultipleZombieSpawners() {
        DungeonManiaController dmc = new DungeonManiaController();
        DungeonResponse res = dmc.newGame("d_zombieTest_multiSpawners", "c_zombieTest_spawnEveryTick");
        int currZombCount = 2;

        assertEquals(getZombSize(res), currZombCount);

        for (int i = 0; i < 10; i++) {
            res = dmc.tick(Direction.UP);
            // since there are 3 zombie toast spawners and the spawn_rate is 1, 3 new zombies should be created per tick.
            currZombCount += 3;
        }

        assertEquals(getZombSize(res), currZombCount);
    }

    // Zombie movement tests:

    @Test
    @DisplayName("Test zombies can only move in one position")
    public void testZombieCanOnlyMoveInOnePos() {
        DungeonManiaController dmc = new DungeonManiaController();
        DungeonResponse res = dmc.newGame("d_zombieTest_moveOnlyOnePos", "c_zombieTest_moveOnlyOnePos");

        res = dmc.tick(Direction.LEFT);
        res = dmc.tick(Direction.LEFT);
        
        assertEquals(getZombSize(res), 2);
    }

    @Test
    @DisplayName("Test zombies cannot move through walls, boulders and locked doors")
    public void testZombieMoveRestrictions() {
        DungeonManiaController dmc = new DungeonManiaController();
        DungeonResponse res = dmc.newGame("d_zombieTest_moveRestrictions", "c_zombieTest_spawnEveryTick");

        res = dmc.tick(Direction.DOWN);

        Position z1Pos1 = getZombPos(0, res);
        assertTrue(z1Pos1.equals(new Position(2, 2)));

        res = dmc.tick(Direction.DOWN);
        Position z1Pos2 = getZombPos(0, res);
        assertTrue(z1Pos2.equals(new Position(3, 2)) || z1Pos2.equals(new Position(2, 2)));

        Position z2Pos1 = getZombPos(1, res);
        assertTrue(z2Pos1.equals(new Position(2, 2)));

        res = dmc.tick(Direction.DOWN);
        Position z2Pos2 = getZombPos(1, res);
        assertTrue(z2Pos2.equals(new Position(3, 2)) || z2Pos2.equals(new Position(2, 2)));

        Position z3Pos1 = getZombPos(2, res);
        assertTrue(z3Pos1.equals(new Position(2, 2)));

    }

    @Test
    @DisplayName("Test zombies can only move up, down, right, left or stay where they are")
    public void testZombieCardinalMovements() {
        DungeonManiaController dmc = new DungeonManiaController();
        DungeonResponse res = dmc.newGame("d_zombieTest_spawnSuccess", "c_zombieTest_spawnEveryTick");

        Position spawnerPos = getEntities(res, "zombie_toast_spawner").get(0).getPosition();
        List<Position> possibleSpawnPos = getSpawnLocations(res, spawnerPos);

        res = dmc.tick(Direction.DOWN);
        Position z1Pos1 = getZombPos(0, res);
        assertTrue(possibleSpawnPos.contains(z1Pos1));

        res = dmc.tick(Direction.DOWN);
        Position z1Pos2 = getZombPos(0, res);
        Position z2Pos1 = getZombPos(1, res);
        assertTrue(possibleSpawnPos.contains(z2Pos1));
        assertTrue(Position.isAdjacent(z1Pos1, z1Pos2) || z1Pos1 == z1Pos2);

        res = dmc.tick(Direction.DOWN);
        Position z1Pos3 = getZombPos(0, res);
        Position z2Pos2 = getZombPos(1, res);
        Position z3Pos1 = getZombPos(2, res);
        assertTrue(possibleSpawnPos.contains(z3Pos1));
        assertTrue(Position.isAdjacent(z1Pos2, z1Pos3) || z1Pos2 == z1Pos3);
        assertTrue(Position.isAdjacent(z2Pos1, z2Pos2) || z2Pos1 == z2Pos2);
    }


}
