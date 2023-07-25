package Zeze.World.Astar;

import java.util.Deque;
import Zeze.Serialize.Vector3;

/**
 * 限制搜索范围.
 */
public class ResourceMapView implements IResourceMap {
	private final NodeIndex offset;
	private final IResourceMap map;
	private final int width;
	private final int height;

	public ResourceMapView(NodeIndex center, NodeIndex range, IResourceMap map) {
		if (range.x <= 0 || range.z <= 0)
			throw new RuntimeException("invalid range. " + range);

		var x = center.x - range.x;
		var z = center.z - range.z;
		this.offset = map.toIndex(x, z);
		this.map = map;
		this.width = (range.x << 1) + 1;
		this.height= (range.z << 1) + 1;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public float getUnitWidth() {
		return map.getUnitWidth();
	}

	@Override
	public float getUnitHeight() {
		return map.getUnitHeight();
	}

	@Override
	public boolean walkable(int x, int z) {
		if (x >= width || z >= height)
			return false;

		return map.walkable(x + offset.x, z + offset.z);
	}

	private NodeIndex addOffset(NodeIndex cur) {
		return new NodeIndex(cur.x + offset.x, cur.z + offset.z, cur.yIndex);
	}

	private NodeIndex subOffset(NodeIndex cur) {
		return new NodeIndex(cur.x - offset.x, cur.z - offset.z, cur.yIndex);
	}

	@Override
	public boolean walkable(NodeIndex from, int toX, int toZ, int toYIndex) {
		if (from.x >= width || from.z >= height || toX >= width || toZ >= height)
			return false;

		return map.walkable(
				addOffset(from),
				toX + offset.x, toZ + offset.z, toYIndex);
	}

	@Override
	public void traverseNeighbors(IAstar astar, Node current, Node target) {
		// save
		var currentIndex = current.index;
		var targetIndex = target.index;
		// add offset
		current.index = addOffset(currentIndex);
		target.index = addOffset(targetIndex);
		// call real
		map.traverseNeighbors(new AstarWrapper(astar, currentIndex, targetIndex), current, target);
		// restore
		current.index = currentIndex;
		target.index = targetIndex;
	}

	class AstarWrapper implements IAstar {
		private final IAstar astar;
		private final NodeIndex current;
		private final NodeIndex target;

		AstarWrapper(IAstar astar, NodeIndex current, NodeIndex target) {
			this.astar = astar;
			this.current = current;
			this.target = target;
		}

		@Override
		public boolean find(IResourceMap map, Vector3 fromV3, Vector3 toV3, Deque<Node> path) {
			return astar.find(map, fromV3, toV3, path);
		}

		@Override
		public void traverseCorner(IResourceMap map, Node current, Node target, int cx, int cz, int cost, int cx0, int cz0, int cx1, int cz1) {
			// save
			var saveCurrent = current.index;
			var saveTarget = target.index;
			// change to old
			current.index = this.current;
			target.index = this.target;
			// agent
			astar.traverseCorner(map, current, target, cx, cz, cost, cx0, cz0, cx1, cz1);
			// restore
			current.index = saveCurrent;
			target.index = saveTarget;
		}

		@Override
		public void traverseCross(IResourceMap map, Node current, Node target, NodeIndex to, int cost) {
			// save
			var saveCurrent = current.index;
			var saveTarget = target.index;
			// change to old
			current.index = this.current;
			target.index = this.target;
			to = subOffset(to);
			// agent
			astar.traverseCross(map, current, target, to, cost);
			// restore
			current.index = saveCurrent;
			target.index = saveTarget;
		}
	}
}
