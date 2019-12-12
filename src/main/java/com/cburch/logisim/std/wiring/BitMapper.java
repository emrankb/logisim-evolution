package com.cburch.logisim.std.wiring;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.*;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.tools.key.BitWidthConfigurator;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

//TODO Add distribute ascending and descending
//TODO Add HDL compilation
//TODO Migrate propagation logic to support bidirectional use and proper wire integration
//TODO Add some distinguishing visual features
//TODO Implement facing
public class BitMapper extends InstanceFactory {

	private enum Ports{
		A(() -> new Port(-20, 0, Port.INPUT, StdAttr.WIDTH), "A", Direction.WEST),
		B(() -> new Port(20, 0, Port.OUTPUT, StdAttr.WIDTH), "B", Direction.EAST);

		private Supplier<Port> newInstance;
		String label;
		Direction direction;

		Ports(Supplier<Port> newInstance, String label, Direction direction){
			this.newInstance = newInstance;
			this.label = label;
			this.direction = direction;
		}

		Port getNewInstance(){
			return this.newInstance.get();
		}

		void drawPort(InstancePainter painter){
			if(this.label != null && this.direction != null){
				painter.drawPort(this.ordinal(), this.label, this.direction);
			}else{
				painter.drawPort(this.ordinal());
			}
		}

	}

	public BitMapper() {
		super("Bit Mapper", S.getter("bitMapperComponent"));
		this.setIconName("mapper.png");
		this.setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
		this.setOffsetBounds(Bounds.create(-20, -20, 40, 40));
		this.setPorts(Arrays.stream(BitMapper.Ports.values())
				.map(Ports::getNewInstance)
				.collect(Collectors.toList()));
	}

	protected void configureNewInstance(Instance instance){
		instance.addAttributeListener();
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if(attr == StdAttr.FACING){
			//TODO implement
		}else if(attr == StdAttr.WIDTH){
			instance.fireInvalidated();
		}else if(attr instanceof BitMapperAttributes.BitAssociationAttribute){
			instance.fireInvalidated();
		}
	}

	@Override
	public AttributeSet createAttributeSet() {
		return new BitMapperAttributes();
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		painter.drawBounds();
		painter.getGraphics().setColor(Color.GRAY);
		List.of(Ports.values()).forEach(p -> p.drawPort(painter));
	}

	@Override
	public void propagate(InstanceState state) {
//		if(!(state.isPortConnected(Ports.A.ordinal()) && state.isPortConnected(Ports.B.ordinal()))){
//			return;
//		}
//		List<Integer> mapAToB = state.getAttributeValue(BitMapperAttributes.MAP_A_TO_B_ATTRIBUTE);
		List<Integer> mapBToA = state.getAttributeValue(BitMapperAttributes.MAP_B_TO_A_ATTRIBUTE);
		Value valueA = state.getPortValue(Ports.A.ordinal());
		Value[] valueB = Value.createUnknown(BitWidth.create(mapBToA.size())).getAll();
		IntStream.range(0, mapBToA.size()).forEach(i -> valueB[i] = valueA.get(mapBToA.get(i)));
		state.setPort(Ports.B.ordinal(), Value.create(valueB), 0);
	}
}
