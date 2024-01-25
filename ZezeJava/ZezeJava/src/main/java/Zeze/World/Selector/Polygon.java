package Zeze.World.Selector;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import Zeze.Serialize.Vector3;
import Zeze.World.Cube;
import Zeze.World.CubeIndex;
import Zeze.World.CubeMap;
import Zeze.World.Entity;
import Zeze.World.Graphics.Graphics2D;
import Zeze.World.Graphics.Graphics3D;
import Zeze.World.ICompute;
import Zeze.World.ISelector;

/**
 * 选择多边形限定的范围的cubes和entities。
 */
public class Polygon implements ISelector {
	private final java.util.List<Vector3> polygon;
	private final boolean isConvex;

	// 优化，初始化的时候算出包围盒，以后用来更快速的判断。
	private final Graphics2D.BoxFloat box;

	/**
	 * 构造。
	 * @param polygon 以(0, 0, 0)为原点。
	 * @param isConvex 是否凸多边形。
	 */
	public Polygon(java.util.List<Vector3> polygon, boolean isConvex) {
		this.polygon = polygon;
		this.isConvex = isConvex;
		this.box = new Graphics2D.BoxFloat(polygon);
	}

	static class PolygonContext extends ICompute.Context {
		public java.util.List<Vector3> worldPolygon;
	}

	@Override
	public ICompute.Context beginSelect() {
		return new PolygonContext();
	}

	@Override
	public SortedMap<CubeIndex, Cube> cubes(ICompute.Context context) {
		var origin = context.compute.caster();
		var myContext = (PolygonContext)context;
		var newPolygon = Graphics3D.transformPolygon(polygon, Vector3.ZERO, Graphics3D.DirectionToZ,
				origin.getBean().getMoving().getPosition(), origin.getBean().getMoving().getDirect());

		// 在Context里面保存结果，以后需要的时候可以直接使用，不用再次计算。
		context.cubes = CubeMap.polygon2d(origin.getCube().map, newPolygon, isConvex);
		myContext.worldPolygon = newPolygon;
		return context.cubes;
	}

	/**
	 * 选择多边形内的实体。
	 * 【对于mmo战斗逻辑来说，这些实体还需要进行敌我识别过滤。是否在这里抽象还需确认。】
	 *
	 * @param context 计算上下文
	 * @return entities in polygon.
	 */
	@Override
	public List<Entity> entities(ICompute.Context context) {
		var myContext = (PolygonContext)context;

		var entities = new ArrayList<Entity>();
		var worldBox = box.add(context.compute.caster().getBean().getMoving().getPosition());
		var cubes = context.cubes;
		for (var cube : cubes.values()) {
			for (var entity : cube.objects.values()) {
				var position = entity.getBean().getMoving().getPosition();
				if (!worldBox.inside(position)) // 快速判断，优化，如果不在包围盒内，肯定不在多边形内。
					continue;

				if (isConvex) {
					if (Graphics2D.insideConvexPolygon(position, myContext.worldPolygon))
						entities.add(entity);
				} else {
					if (Graphics2D.insidePolygon(position, myContext.worldPolygon))
						entities.add(entity);
				}
			}
		}
		return entities;
	}
}
