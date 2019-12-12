package com.cburch.logisim.std.wiring;

import com.cburch.logisim.data.*;
import com.cburch.logisim.gui.generic.ComboBox;
import com.cburch.logisim.instance.StdAttr;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.cburch.logisim.std.Strings.S;

public class BitMapperAttributes extends AbstractAttributeSet {

	public static class BitAssociationAttribute extends Attribute<Integer> implements Comparable<BitAssociationAttribute> {
		private int bit;
		private int width;

		private BitAssociationAttribute(int bit, int width){
			super("bit" + bit, S.getter("bitMapperBitAttribute", bit + ""));
			this.bit = bit;
			this.width = width;
		}

		@Override
		public Integer parse(String value) {
			return Integer.parseInt(value);
		}

		@Override
		public int compareTo(BitAssociationAttribute attribute) {
			return this.bit - attribute.bit;
		}

		public int getBit(){
			return this.bit;
		}

		public java.awt.Component getCellEditor(Integer value){
			ComboBox comboBox = new ComboBox<>(IntStream.range(0, this.width).boxed().toArray());
			comboBox.setSelectedIndex(value);
			comboBox.setMaximumRowCount(this.width);
			return comboBox;
		}

		@Override
		public String toDisplayString(Integer value) {
			return value.toString();
		}
	}

	private NavigableMap<BitAssociationAttribute, Integer> associations;
	private List<Map.Entry<Attribute<?>, Object>> baseAttributes;

	public static final Attribute<List<Integer>> MAP_A_TO_B_ATTRIBUTE = new Attribute<>() {
		@Override
		public List<Integer> parse(String value) {
			return null;
		}
	};

	public static final Attribute<List<Integer>> MAP_B_TO_A_ATTRIBUTE = new Attribute<>() {
		@Override
		public List<Integer> parse(String value) {
			return null;
		}
	};

	private List<Integer> mapAToB, mapBToA;

	BitMapperAttributes(){
		this.baseAttributes = new ArrayList<>();
		this.baseAttributes.add(new AbstractMap.SimpleEntry<>(StdAttr.FACING, Direction.EAST));
		this.baseAttributes.add(new AbstractMap.SimpleEntry<>(StdAttr.WIDTH, BitWidth.create(8)));
		this.resize(8);
	}

	private Map<Attribute<?>, Object> baseAttributesMap(){
		return new AbstractMap<>() {
			@Override
			public Object put(Attribute<?> key, Object value) {
				if(!this.containsKey(key)){
					throw new IllegalArgumentException();
				}
				Object old = this.get(key);
				//noinspection OptionalGetWithoutIsPresent
				this.entrySet().stream().filter(e -> e.getKey().equals(key)).findFirst().get().setValue(value);
				return old;
			}

			@Override
			public Set<Entry<Attribute<?>, Object>> entrySet() {
				return new AbstractSet<>() {
					@Override
					public Iterator<Entry<Attribute<?>, Object>> iterator() {
						return BitMapperAttributes.this.baseAttributes.iterator();
					}

					@Override
					public int size() {
						return BitMapperAttributes.this.baseAttributes.size();
					}
				};
			}
		};
	}

	private void resize(int bitWidth){
		this.baseAttributesMap().put(StdAttr.WIDTH, BitWidth.create(bitWidth));
		this.associations = new TreeMap<>();
		IntStream.range(0, bitWidth)
				.forEach(i -> this.associations.put(new BitAssociationAttribute(i, bitWidth), i));
		fireAttributeListChanged();
		this.buildMaps();
	}

	private void setAssociation(BitAssociationAttribute a, int b){
		if(b < 0 || b >= this.associations.size() || !this.associations.containsKey(a)){
			throw new IllegalArgumentException();
		}
		this.associations.put(
				this.associations.entrySet().stream()
						.filter(e -> e.getValue() == b).findFirst()
						.orElseThrow(IllegalStateException::new)
						.getKey(),
				this.associations.get(a));
		this.associations.put(a, b);
		this.buildMaps();
	}

	private void buildMaps(){
		this.mapAToB = new ArrayList<>(this.associations.values());
		TreeMap<Integer, Integer> treeMap = new TreeMap<>();
		IntStream.range(0, this.mapAToB.size()).forEach(i -> treeMap.put(this.mapAToB.get(i), i));
		this.mapBToA = new ArrayList<>(treeMap.values());
	}


	@Override
	protected void copyInto(AbstractAttributeSet destObject) {
		BitMapperAttributes dest = (BitMapperAttributes) destObject;
		dest.baseAttributes = new ArrayList<>(this.baseAttributes);
		dest.associations = new TreeMap<>(this.associations);
		dest.buildMaps();
	}

	@Override
	public List<Attribute<?>> getAttributes() {
		return Stream.concat(
				this.baseAttributes.stream().map(Map.Entry::getKey),
				this.associations.keySet().stream())
				.collect(Collectors.toList());
	}

	@Override
	public <V> V getValue(Attribute<V> attr) {
		if(attr == BitMapperAttributes.MAP_A_TO_B_ATTRIBUTE){
			//noinspection unchecked
			return (V) Collections.unmodifiableList(this.mapAToB);
		}
		if(attr == BitMapperAttributes.MAP_B_TO_A_ATTRIBUTE){
			//noinspection unchecked
			return (V) Collections.unmodifiableList(this.mapBToA);
		}
		if(this.baseAttributesMap().containsKey(attr)) {
			//noinspection unchecked
			return (V) this.baseAttributesMap().get(attr);
		}
		if(attr instanceof BitAssociationAttribute){
			//noinspection unchecked
			return (V) this.associations.get(attr);
		}
		return null;
	}

	@Override
	public <V> void setValue(Attribute<V> attr, V value) {
		if(this.baseAttributesMap().containsKey(attr)){
			this.baseAttributesMap().put(attr, value);
			if(this.associations.size() != ((BitWidth)this.baseAttributesMap().get(StdAttr.WIDTH)).getWidth()){
				this.resize(((BitWidth)this.baseAttributesMap().get(StdAttr.WIDTH)).getWidth());
			}
		}else if(attr instanceof BitAssociationAttribute){
			this.setAssociation((BitAssociationAttribute)attr, (Integer) value);
		}else{
			throw new IllegalArgumentException();
		}
		this.fireAttributeValueChanged(attr, value, null); //TODO fix
	}
}
