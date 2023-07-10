package Zeze.World;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Serialize.Vector3;

/**
 * 把三维空间划分成一个个相邻的Cube。
 * 地图中的玩家或者物品记录在所在的Cube中。
 * 用来快速找到某个坐标周围的实体。
 */
public class CubeMap {
	private final ConcurrentHashMap<CubeIndex, Cube> cubes = new ConcurrentHashMap<>();
	private final int cubeX;
	private final int cubeY; // 2d 切割时，cubeY 为 0.
	private final int cubeZ;

	private IAoi aoi;

	// x-size of cube
	public final int getCubeX() {
		return cubeX;
	}

	// y-size of cube
	public final int getCubeY() {
		return cubeY;
	}

	// z-size of cube
	public final int getCubeZ() {
		return cubeZ;
	}

	public IAoi getAoi() {
		return aoi;
	}

	public void setAoi(IAoi aoi) {
		this.aoi = aoi;
	}

	public final CubeIndex toIndex(Vector3 vector3) {
		var x = (long)(vector3.x / this.cubeX);
		// 允许最少1维，1维用来测试。
		var y = this.cubeY != 0 ? (long)(vector3.y / this.cubeY) : 0;
		var z = this.cubeZ != 0 ? (long)(vector3.z / this.cubeZ) : 0;
		return new CubeIndex(x, y, z);
	}

	public final CubeIndex toIndex(float _x, float _y, float _z) {
		var x = (long)(_x / this.cubeX);
		var y = this.cubeY != 0 ? (long)(_z / this.cubeY) : 0;
		var z = this.cubeZ != 0 ? (long)(_y / this.cubeZ) : 0;
		return new CubeIndex(x, y, z);
	}

	/**
	 * 构造地图实例-2d切割，参数为切割长宽。
	 * 2d切割时，使用x,z。y为0. 符合3d引擎的一般模式。
	 * @param gridX 切割长度
	 * @param gridZ 切割宽度
	 */
	public CubeMap(int gridX, int gridZ) {
		this(gridX, 0, gridZ);
	}

	/**
	 * 构造地图实例-3d切割，参数为切割长宽。
	 * @param gridX 切割长度
	 * @param gridY 切割宽度
	 * @param gridZ 切割高度
	 */
	public CubeMap(int gridX, int gridY, int gridZ) {
		if (gridX <= 0)
			throw new IllegalArgumentException("cubeSizeX <= 0");
		if (gridY < 0)
			throw new IllegalArgumentException("cubeSizeY < 0");
		if (gridZ < 0) // gridZ 可以为0，表示2d切割。
			throw new IllegalArgumentException("cubeSizeZ <= 0");

		this.cubeX = gridX;
		this.cubeY = gridY;
		this.cubeZ = gridZ;
	}

	public final Cube getOrAdd(CubeIndex index) {
		return cubes.computeIfAbsent(index, Cube::new);
	}

	public final void collect(SortedMap<CubeIndex, Cube> result, CubeIndex index) {
		result.put(index, getOrAdd(index));
	}

	////////////////////////////////////////////////////////////
	// 2d
	// 选出一个封闭多边形包含的cube。
	// points，两两之间是一个线段，首尾连起来。
	public final SortedMap<CubeIndex, Cube> polygon2d(java.util.List<Vector3> points, boolean convex) {
		// cube-box 包围体
		var boxMinX = Long.MAX_VALUE;
		var boxMinZ = Long.MAX_VALUE;

		var boxMaxX = Long.MIN_VALUE;
		var boxMaxZ = Long.MIN_VALUE;

		// 转换成把CubeIndex看成点的多边形。
		var cubePoints = new ArrayList<CubeIndex>();
		for (var point : points) {
			var index = toIndex(point);

			if (index.x < boxMinX) boxMinX = index.x;
			if (index.x > boxMaxX) boxMaxX = index.x;
			if (index.z < boxMinZ) boxMinZ = index.z;
			if (index.z > boxMaxZ) boxMaxZ = index.z;

			// 第一个cube或者跟最近的一个不相等的时候加入。
			if (cubePoints.isEmpty() || !cubePoints.get(cubePoints.size() - 1).equals(index))
				cubePoints.add(index);
		}

		//System.out.println(cubePoints);
		//System.out.println(boxMinX + "~" + boxMaxX + ", " + boxMinZ + "~" + boxMaxZ);
		var result = new TreeMap<CubeIndex, Cube>();
		if (cubePoints.size() < 3) {
			for (var index : cubePoints)
				collect(result, index);
			return result;
			// 不可能构成多边形。这实际上是多数情况。
		}

		// 边线用画线法得到。【因为后面的多边形内判断方式得不到边线的cube??? 需确认，另外画线法比后面的判断快】
		for (int i = 0, j = cubePoints.size() - 1; i < cubePoints.size(); j = i, ++i) {
			var p1 = cubePoints.get(i);
			var p2 = cubePoints.get(j);
			//System.out.println("bresenham2d " + p1 + "->" + p2);
			Graphics2D.bresenham2d(p1.x, p1.z, p2.x, p2.z, (x, z) -> {
				var index = new CubeIndex(x, 0, z);
				//System.out.println("collect " + index);
				collect(result, index);
			});
		}

		// 【注意】绝大多数情况，box都是空的。所以根本不会进行多边形判断。
		// box 边线上面已经处理了，这里判空一下。
		var boxEmpty = boxMaxX - boxMinX <= 1 || boxMaxZ - boxMinZ <= 1;
		if (boxEmpty)
			return result;

		if (convex) {
			for (var i = boxMinX + 1; i < boxMaxX; ++i) {
				for (var k = boxMinZ + 1; k < boxMaxZ; ++k) {
					var index = new CubeIndex(i, 0, k);
					if (Graphics2D.insideConvexPolygon(index, cubePoints))
						collect(result, index);
				}
			}
		} else {
			for (var i = boxMinX + 1; i < boxMaxX; ++i) {
				for (var k = boxMinZ + 1; k < boxMaxZ; ++k) {
					var index = new CubeIndex(i, 0, k);
					if (Graphics2D.insidePolygon(index, cubePoints))
						collect(result, index);
				}
			}
		}
		return result;
	}

	// 选出position开始，面向direct方向，distance距离，直线路径经过的cube。
	public final SortedMap<CubeIndex, Cube> line2d(Vector3 position, Vector3 direct, int distance) {
		// todo 计算结束cube的索引。
		// direct 向量有约定？就是归一化什么的？不清楚。
		var endX = position.x + distance * direct.x;
		var endY = position.x + distance * direct.x;

		var endIndex = toIndex(endX, endY, 0);
		var beginIndex = toIndex(position);

		var result = new TreeMap<CubeIndex, Cube>();
		Graphics2D.bresenham2d(beginIndex.x, beginIndex.z, endIndex.x, endIndex.z,
				(x, y) -> collect(result, new CubeIndex(x, y)));
		return result;
	}

	////////////////////////////////////////////////////////////
	// 3d
	public final SortedMap<CubeIndex, Cube> center(Vector3 center, int rangeX, int rangeY, int rangeZ) {
		return center(toIndex(center), rangeX, rangeY,rangeZ);
	}

	public final SortedMap<CubeIndex, Cube> center(float centerX, float centerY, float centerZ,
													 int rangeX, int rangeY, int rangeZ) {
		return center(toIndex(centerX, centerY, centerZ), rangeX, rangeY,rangeZ);
	}

	public final SortedMap<CubeIndex, Cube> center(CubeIndex center, int rangeX, int rangeY, int rangeZ) {
		var result = new TreeMap<CubeIndex, Cube>();
		for (long i = center.x - rangeX; i <= center.x + rangeX; ++i) {
			for (long j = center.y - rangeY; j <= center.y + rangeY; ++j) {
				for (long k = center.z - rangeZ; k <= center.z + rangeZ; ++k) {
					var index = new CubeIndex(i, j, k);
					result.put(index, getOrAdd(index));
				}
			}
		}
		return result;
	}
}
