import java.util.ArrayList;
import java.util.List;

public class MyBot_Simple {
    private static int myID;

    public static void main(String[] args) throws java.io.IOException {

        final InitPackage iPackage = Networking.getInit();
        // final int myID = iPackage.myID;
        myID = iPackage.myID;
        final GameMap gameMap = iPackage.map;

        Networking.sendInit("MyJavaBot");

        while (true) {
            List<Move> moves = new ArrayList<Move>();

            Networking.updateFrame(gameMap);

            for (int y = 0; y < gameMap.height; y++) {
                for (int x = 0; x < gameMap.width; x++) {
                    final Location location = gameMap.getLocation(x, y);
                    Move move = getMove(gameMap, location);
                    if (move != null) {
                        moves.add(move);
                    }
                }
            }
            Networking.sendFrame(moves);
        }
    }

    private static Move getMove(final GameMap map, final Location loc) {
        final Site site = loc.getSite();
        if (site.owner != myID) return null;
        if (site.strength < site.production*5) return new Move(loc, Direction.STILL);

        int minDist = Math.max(map.height, map.width);
        Direction shortestDir = Direction.NORTH;

        // Iterate through all directions, finding the shortest route.
        for (int i = 1; i < Direction.DIRECTIONS.length; i++) {
            Direction direction = Direction.DIRECTIONS[i];
            int dist = getDistToEnemy(map, loc, direction);
            if (dist < minDist) {
                minDist = dist;
                shortestDir = direction;
            }
        }

        // If this is an edge node, find the best direction to go
        if (minDist == 1) {
            shortestDir = getWeakestEnemyDirection(map, loc);
        }

        return new Move(loc, shortestDir);
    }

    private static int getDistToEnemy(final GameMap map, Location loc, final Direction dir) {
        int dist = 0;
        int limit = (dir == Direction.NORTH || dir == Direction.SOUTH) ? map.height : map.width;
        while (dist < limit && loc.getSite().owner == myID) {
            dist++;
            loc = map.getLocation(loc, dir);
        }
        return dist;
    }

    // Move toward the weakest neighbor node, or stay still
    private static Direction getWeakestEnemyDirection(final GameMap map, final Location loc) {
        Direction dir = Direction.STILL;
        int minStrength = 256;

        for (int i = 1; i < Direction.DIRECTIONS.length; i++) {
            Direction direction = Direction.DIRECTIONS[i];
            Site neighbor = map.getLocation(loc, direction).getSite();
            if (neighbor.owner != myID && (neighbor.strength+20 < loc.getSite().strength || neighbor.strength > 200 && neighbor.strength <= loc.getSite().strength)) {
                if (neighbor.strength < minStrength) {
                    minStrength = neighbor.strength;
                    dir = direction;
                }
            }
        }

        return dir;
    }
}
