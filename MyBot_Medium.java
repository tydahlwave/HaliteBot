import java.util.ArrayList;
import java.util.List;

public class MyBot_Medium {
    private static int myID;
    private static int neutralID = 0;
    private static ArrayList<Location> combatPositions;
    private static double averageProductionRate;
    private static double averageProductionRateCoeff; // Used to calculate heuristic
    private static boolean isInCombat = false;
    private static double myTotalProduction = 0;
    private static double myTotalStrength = 0;

    public static void main(String[] args) throws java.io.IOException {

        final InitPackage iPackage = Networking.getInit();
        // final int myID = iPackage.myID;
        myID = iPackage.myID;
        final GameMap gameMap = iPackage.map;

        // Perform initial setup (max 15 secs)
        setup(gameMap);

        Networking.sendInit("MyBot_Medium");

        // Run game loop (max 1 sec)
        while (true) {
            Networking.updateFrame(gameMap);
            update(gameMap);
            List<Move> moves = getMoves(gameMap);
            Networking.sendFrame(moves);
        }
    }

    private static void setup(final GameMap map) {
        averageProductionRate = getAverageProductionRate(map);
        averageProductionRateCoeff = averageProductionRate / 255.0;
    }

    private static void update(final GameMap map) {
        checkIsInCombat(map);
        myTotalProduction = getTotalProductionRate(map, myID);
        myTotalStrength = getTotalStrength(map, myID);
    }

    // Calculate value of a node
    private static double heuristic(final GameMap map, final Location myLoc, final Location target) {
        final Site targetSite = target.getSite();

        // production rate scale: 0 - 1+, with 1 being average production for map
        double prodCoeff = ((double)targetSite.production / 255.0) / averageProductionRateCoeff;
        // strength scale: 0 - 1, with 1 being 0 strength
        double strCoeff = 1 - ((double)targetSite.strength / 255.0);
        // distance scale: -0 - 1, with 1 being 0 distance, and 0 being 10 distance
        double distCoeff = (5.0 - map.getDistance(myLoc, target)) / 5.0;
        // enemy scale: 0 or 0.2, with 0.5 being enemy
        double enemyCoeff = (targetSite.owner != myID && targetSite.owner != neutralID) ? 1 : 0;

        return prodCoeff + strCoeff + distCoeff + enemyCoeff;
        // if (targetSite.strength > 0) {
        //     return targetSite.production / targetSite.strength;
        // } else {
        //     return targetSite.production;
        // }
    }

    private static Move getMoveForLocation(final GameMap map, final Location loc) {
        final Site site = loc.getSite();
        if (site.owner != myID) return null;
        if (site.strength < site.production*5 && site.strength < 255) return new Move(loc, Direction.STILL);

        double bestValue = -10;
        Location chosenTarget = loc;
        Direction direction = Direction.NORTH;

        // Check every node on the map to find the best target for this node
        for (int y = 0; y < map.height; y++) {
            for (int x = 0; x < map.width; x++) {
                final Location target = map.getLocation(x, y);
                final Site targetSite = target.getSite();
                if (targetSite.owner == myID) continue;
                double value = heuristic(map, loc, target);
                if (value > bestValue) {
                    bestValue = value;
                    chosenTarget = target;
                }
            }
        }
        direction = getDirectionForTarget(map, loc, chosenTarget);

        // Check that the move is valid
        direction = checkValidMove(map, loc, direction);

        return new Move(loc, direction);
    }

    private static Direction checkValidMove(final GameMap map, final Location loc, final Direction dir) {
        Location target = map.getLocation(loc, dir);
        Site targetSite = target.getSite();
        Site mySite = loc.getSite();

        // If moving this direction would cause a loss of total strength, stay still instead
        // if (targetSite.owner == myID && targetSite.strength + mySite.strength > 300) {
        //     return Direction.STILL;
        // }
        // If cannot defeat piece by moving in this direction, stay still
        if (targetSite.strength > mySite.strength) {
            return Direction.STILL;
        }
        
        return dir;
    }

    private static Direction getDirectionForTarget(final GameMap map, final Location myLoc, final Location target) {
        int dx = Math.abs(target.x - myLoc.x);
        int dy = Math.abs(target.y - myLoc.y);
        boolean reverseHorz = false;
        boolean reverseVert = false;

        if (dx > map.width / 2.0) {
            reverseHorz = true;
            dx = map.width - dx;
        }
        if (dy > map.height / 2.0) {
            reverseVert = true;
            dy = map.height - dy;
        }

        if (dy > dx) {
            if (target.y > myLoc.y) {
                return (reverseVert) ? Direction.NORTH : Direction.SOUTH;
            } else {
                return (!reverseVert) ? Direction.NORTH : Direction.SOUTH;
            }
        } else {
            if (target.x > myLoc.x) {
                return (reverseHorz) ? Direction.WEST : Direction.EAST;
            } else {
                return (!reverseHorz) ? Direction.WEST : Direction.EAST;
            }
        }
    }

    private static List<Move> getMoves(final GameMap map) {
        List<Move> moves = new ArrayList<Move>();

        for (int y = 0; y < map.height; y++) {
            for (int x = 0; x < map.width; x++) {
                final Location location = map.getLocation(x, y);
                Move move = getMoveForLocation(map, location);
                if (move != null) {
                    moves.add(move);
                }
            }
        }

        // Prune moves of duplicates (don't want two blocks moving to same square if already at max strength)
        // List<Move> filteredMoves = new ArrayList<Move>();
        // List<Location> locations = new ArrayList<Location>();
        // for (Move move : moves) {
        //     final Location loc = map.getLocation(move.loc, move.dir);
        //     if (!locations.contains(loc)) {
        //         locations.add(loc);
        //         filteredMoves.add(move);
        //     }
        // }

        return moves;//filteredMoves;
    }

    // private static Move getMove(final GameMap map, final Location loc) {
    //     final Site site = loc.getSite();
    //     if (site.owner != myID) return null;
    //     if (site.strength < site.production*5) return new Move(loc, Direction.STILL);

    //     int minDist = Math.max(map.height, map.width);
    //     Direction shortestDir = Direction.NORTH;

    //     // Iterate through all directions, finding the shortest route.
    //     for (int i = 1; i < Direction.DIRECTIONS.length; i++) {
    //         Direction direction = Direction.DIRECTIONS[i];
    //         int dist = getDistToEnemy(map, loc, direction);
    //         if (dist < minDist) {
    //             minDist = dist;
    //             shortestDir = direction;
    //         }
    //     }

    //     // If this is an edge node, find the best direction to go
    //     if (minDist == 1) {
    //         shortestDir = getWeakestEnemyDirection(map, loc);
    //     }

    //     return new Move(loc, shortestDir);
    // }

    // private static int getDistToEnemy(final GameMap map, Location loc, final Direction dir) {
    //     int dist = 0;
    //     int limit = (dir == Direction.NORTH || dir == Direction.SOUTH) ? map.height : map.width;
    //     while (dist < limit && loc.getSite().owner == myID) {
    //         dist++;
    //         loc = map.getLocation(loc, dir);
    //     }
    //     return dist;
    // }

    // // Move toward the weakest neighbor node, or stay still
    // private static Direction getWeakestEnemyDirection(final GameMap map, final Location loc) {
    //     Direction dir = Direction.STILL;
    //     int minStrength = 256;

    //     for (int i = 1; i < Direction.DIRECTIONS.length; i++) {
    //         Direction direction = Direction.DIRECTIONS[i];
    //         Site neighbor = map.getLocation(loc, direction).getSite();
    //         if (neighbor.owner != myID && (neighbor.strength+20 < loc.getSite().strength || neighbor.strength > 200 && neighbor.strength <= loc.getSite().strength)) {
    //             if (neighbor.strength < minStrength) {
    //                 minStrength = neighbor.strength;
    //                 dir = direction;
    //             }
    //         }
    //     }

    //     return dir;
    // }

    private static double getAverageProductionRate(final GameMap map) {
        int production = 0;
        for (int y = 0; y < map.height; y++) {
            for (int x = 0; x < map.width; x++) {
                production += map.getLocation(x, y).getSite().production;
            }
        }
        return (double)production / (double)(map.height*map.width);
    }

    private static double getAverageProductionRate(final GameMap map, final int owner) {
        int production = 0;
        int nodes = 0;
        for (int y = 0; y < map.height; y++) {
            for (int x = 0; x < map.width; x++) {
                final Site site = map.getLocation(x, y).getSite();
                if (site.owner == owner) {
                    nodes++;
                    production += site.production;
                }
            }
        }
        return (double)production / (double)nodes;
    }

    private static double getTotalProductionRate(final GameMap map, final int owner) {
        int production = 0;
        for (int y = 0; y < map.height; y++) {
            for (int x = 0; x < map.width; x++) {
                final Site site = map.getLocation(x, y).getSite();
                if (site.owner == owner) {
                    production += site.production;
                }
            }
        }
        return (double)production;
    }

    private static double getTotalStrength(final GameMap map, final int owner) {
        int strength = 0;
        for (int y = 0; y < map.height; y++) {
            for (int x = 0; x < map.width; x++) {
                final Site site = map.getLocation(x, y).getSite();
                if (site.owner == owner) {
                    strength += site.strength;
                }
            }
        }
        return (double)strength;
    }

    private static void checkIsInCombat(final GameMap map) {
        combatPositions = new ArrayList<>();
        for (int y = 0; y < map.height-1; y++) {
            for (int x = 0; x < map.width-1; x++) {
                final Site current = map.getLocation(x, y).getSite();
                final Site bottom = map.getLocation(x, y+1).getSite();
                final Site right = map.getLocation(x+1, y).getSite();
                if (current.owner != right.owner) {
                    // If the current node is owned by me, and neighbor is enemy
                    if (current.owner == myID && right.owner != neutralID) {
                        final Location loc = map.getLocation(x+1, y);
                        // If this enemy location is not already recorded, add it
                        if (!combatPositions.contains(loc)) {
                            combatPositions.add(loc);
                        }
                    } else if (right.owner == myID && current.owner != neutralID) {
                        final Location loc = map.getLocation(x, y);
                        // If this enemy location is not already recorded, add it
                        if (!combatPositions.contains(loc)) {
                            combatPositions.add(loc);
                        }
                    }
                }
                if (current.owner != bottom.owner) {
                    // If the current node is owned by me, and neighbor is enemy
                    if (current.owner == myID && bottom.owner != neutralID) {
                        final Location loc = map.getLocation(x, y+1);
                        // If this enemy location is not already recorded, add it
                        if (!combatPositions.contains(loc)) {
                            combatPositions.add(loc);
                        }
                    } else if (bottom.owner == myID && current.owner != neutralID) {
                        final Location loc = map.getLocation(x, y);
                        // If this enemy location is not already recorded, add it
                        if (!combatPositions.contains(loc)) {
                            combatPositions.add(loc);
                        }
                    }
                }
            }
        }
        isInCombat = (combatPositions.size() > 0);
    }

    private static List<Location> neighbors(final GameMap map, final Location loc) {
        List<Location> neighbors = new ArrayList<>();
        neighbors.add(map.getLocation(loc, Direction.NORTH));
        neighbors.add(map.getLocation(loc, Direction.EAST));
        neighbors.add(map.getLocation(loc, Direction.SOUTH));
        neighbors.add(map.getLocation(loc, Direction.WEST));
        return neighbors;
    }
}
