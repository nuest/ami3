/**
 *    Copyright 2011 Peter Murray-Rust et. al.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.contentmine.eucl.stml;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.contentmine.eucl.euclid.Util;
import org.contentmine.eucl.xml.XMLConstants;
import org.contentmine.eucl.xml.XMLUtil;

import nu.xom.Element;
import nu.xom.Node;

/**
 * 
 * <p>
 * supports XSD and derived types generated by software and only instantiated as
 * singletons
 * </p>
 * 
 * @author Peter Murray-Rust
 * @version 5.0
 * 
 */
public class STMLType implements XMLConstants {

	private static final Logger LOG = Logger.getLogger(STMLType.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}

    /** dewisott */
	public final static String NO_BASE = "Cannot find base: ";

	protected String summary = "";
	protected String description = "";
	protected String base = null;
	protected String name = null;
	protected String id = null;
	protected boolean isList = false;
	protected String pattern = null;
	protected int listLength = Integer.MIN_VALUE;
	protected int iMinInclusive = Integer.MIN_VALUE;
	protected int iMinExclusive = Integer.MIN_VALUE;
	protected int iMaxInclusive = Integer.MAX_VALUE;
	protected int iMaxExclusive = Integer.MAX_VALUE;
	protected double dMinInclusive = Double.NaN;
	protected double dMinExclusive = Double.NaN;
	protected double dMaxInclusive = Double.NaN;
	protected double dMaxExclusive = Double.NaN;
	protected STMLType[] subTypes = new STMLType[0];
	protected String[] sEnumerationValues = new String[0];
	protected int[] iEnumerationValues = new int[0];
	protected double[] dEnumerationValues = new double[0];
	protected String javaType;

	protected Element restriction;
	protected Element union;
	protected Element list;

	private Element simpleType;

	/**
	 * default.
	 */
	public STMLType() {
		init();
	}

	private void init() {
		summary = "";
		description = "";
		base = null;
		name = null;
		id = null;
		isList = false;
		pattern = null;
		listLength = Integer.MIN_VALUE;
		iMinInclusive = Integer.MIN_VALUE;
		iMinExclusive = Integer.MIN_VALUE;
		iMaxInclusive = Integer.MAX_VALUE;
		iMaxExclusive = Integer.MAX_VALUE;
		dMinInclusive = Double.NaN;
		dMinExclusive = Double.NaN;
		dMaxInclusive = Double.NaN;
		dMaxExclusive = Double.NaN;
		subTypes = new STMLType[0];
		sEnumerationValues = new String[0];
		iEnumerationValues = new int[0];
		dEnumerationValues = new double[0];
		javaType = null;
	}

	/**
	 * copy constructor.
	 * 
	 * @param st
	 */
	public STMLType(STMLType st) {
	}

	/**
	 * create from XSD simpleType. may have to be called repeatedly untill all
	 * superTypes have been created
	 * 
	 * @param simpleType
	 */
	public STMLType(Element simpleType) {
		init();
		if (!simpleType.getLocalName().equals("simpleType")) {
			throw new RuntimeException(
					"element is not a simpleType, found: "
							+ simpleType.getLocalName());
		}

		this.name = simpleType.getAttributeValue("name");
		this.id = simpleType.getAttributeValue("id");
		this.simpleType = simpleType;
		createUnion();
		// unions are a problem. At present we simply take the first simpleType
		// child
		if (union == null || true) {
			createRestriction();
			createList();
			createBase();
			createDocumentation();
			createPattern();
			createLength();
			createJavaType();
		}
	}

	/**
	 * create min max.
	 */
	public void createMinMaxAndEnumerations() {
		// if (union == null) {
		createMinMax();
		createEnumerations();
		// }
	}

	Element createUnion() {
		List<Node> unions = XMLUtil.getQueryNodes(simpleType,
				".//" + XSD_UNION, XPATH_XSD);
		union = null;
		if (unions.size() == 1) {
			union = (Element) unions.get(0);
		} else if (unions.size() > 1) {
			throw new RuntimeException("More than one union");
		}
		if (union != null) {
			List<Node> nodes = XMLUtil.getQueryNodes(union, "./"
					+ XSD_SIMPLE_TYPE, XPATH_XSD);
			if (nodes.size() != 2) {
				throw new RuntimeException(
						"Union can only have two simpleTypes, found "
								+ nodes.size());
			}
			subTypes = new STMLType[nodes.size()];
			int i = 0;
			for (Node node : nodes) {
				subTypes[i++] = new STMLType((Element) node);
			}
			simpleType = subTypes[0].getSimpleType();
		}
		return union;
	}

	Element createRestriction() {
		List<Node> restrictions = XMLUtil.getQueryNodes(simpleType, ".//"
				+ XSD_RESTRICTION, XPATH_XSD);
		restriction = null;
		if (restrictions.size() == 1) {
			restriction = (Element) restrictions.get(0);
		} else if (restrictions.size() > 1) {
			LOG.error("More than one restriction");
			XMLUtil.debug(simpleType, "CMLTYPE");
		}
		return restriction;
	}

	Element createList() {
		// lists are of two types:
		// indefinite length
		// <xsd:list itemType="atomIDType"/>
		// and
		// specific length
		// <xsd:restriction>
		// <xsd:simpleType>
		// <xsd:list itemType="atomIDType"/>
		// </xsd:simpleType>
		// <xsd:length value="2"/>
		// </xsd:restriction>

		List<Node> lists = null;
		isList = false;
		if (restriction != null) {
			lists = XMLUtil.getQueryNodes(restriction, "./" + XSD_SIMPLE_TYPE
					+ XMLConstants.S_SLASH + XSD_LIST, XPATH_XSD);
			List<Node> lengths = XMLUtil.getQueryNodes(restriction, "./"
					+ XSD_LENGTH, XPATH_XSD);
			if (lengths.size() == 1) {
				Element length = (Element) lengths.get(0);
				try {
					listLength = Integer.parseInt(length
							.getAttributeValue("value"));
				} catch (NumberFormatException nfe) {
					throw new RuntimeException("bad length: " + nfe);
				}
			}
		} else {
			lists = XMLUtil.getQueryNodes(simpleType, ".//" + XSD_LIST,
					XPATH_XSD);
		}
		if (lists.size() == 1) {
			list = (Element) lists.get(0);
			isList = true;
			String baseS = list.getAttributeValue("itemType");
			if (baseS == null) {
				XMLUtil.debug(simpleType, "SIMPLE1");
				throw new RuntimeException("no base for " + name);
			}
			base = baseS;
		} else if (lists.size() > 1) {
			LOG.error("More than one list");
			XMLUtil.debug(simpleType, "SIMPLE2");
		}
		return list;
	}

	String createBase() {
		// base may already have been given
		if (base == null) {
			String baseS = simpleType.getAttributeValue("base");
			base = (restriction == null) ? null : restriction
					.getAttributeValue("base");
			if (baseS == null && base == null) {
				throw new RuntimeException("No base or restriction given");
			} else if (baseS != null) {
				if (base != null) {
					throw new RuntimeException(
							"Cannot give both base attribute and restriction");
				}
				base = baseS;
			}
		}
		return base;
	}

	/**
	 * creates javaType from base. only uses XSD builtins. Id derived from other
	 * types has to be managed from outside this class.
	 * 
	 * @return type
	 */
	String createJavaType() {
		if (javaType == null) {
			if (XSD_INTEGER.equals(base)) {
				javaType = XSD_INTEGER;
			} else if (XSD_NONNEGATIVEINTEGER.equals(base)) {
				javaType = XSD_INTEGER;
				iMinInclusive = 0;
			} else if (XSD_DOUBLE.equals(base) || XSD_FLOAT.equals(base)) {
				javaType = XSD_DOUBLE;
			} else if (XSD_STRING.equals(base)) {
				javaType = XSD_STRING;
			} else if (XSD_BOOLEAN.equals(base)) {
				javaType = XSD_BOOLEAN;
			} else {
			}
		}
		return javaType;
	}

	void createDocumentation() {
		List<Node> docs = XMLUtil.getQueryNodes(simpleType, "./"
				+ XSD_ANNOTATION + XMLConstants.S_SLASH + XSD_DOCUMENTATION, XPATH_XSD);
		if (docs.size() == 0) {
		} else if (docs.size() == 1) {
			Element documentation = (Element) docs.get(0);
			List<Node> summarys = XMLUtil.getQueryNodes(documentation,
					".//*[@class='summary']");
			summary = (summarys.size() == 0) ? null : summarys.get(0)
					.getValue();
			List<Node> descriptions = XMLUtil.getQueryNodes(documentation,
					".//*[@class='description']");
			description = (descriptions.size() == 0) ? null : descriptions.get(
					0).getValue();
		}
	}

	void createPattern() {
		List<Node> patterns = XMLUtil.getQueryNodes(simpleType, "./"
				+ XSD_RESTRICTION + XMLConstants.S_SLASH + XSD_PATTERN, XPATH_XSD);
		if (patterns.size() > 0) {
			pattern = ((Element) patterns.get(0)).getAttributeValue("value");
		}
	}

	void createLength() {
		List<Node> lengths = XMLUtil.getQueryNodes(simpleType, "./"
				+ XSD_RESTRICTION + XMLConstants.S_SLASH + XSD_LENGTH, XPATH_XSD);
		if (lengths.size() > 0) {
			try {
				listLength = Integer.parseInt(((Element) lengths.get(0))
						.getAttributeValue("value"));
			} catch (NumberFormatException e) {
				XMLUtil.debug(simpleType, "SIMPLE3");
				throw new RuntimeException("Bad length " + e);
			}
		}
	}

	void createMinMax() {
		List<Node> minEx = XMLUtil.getQueryNodes(simpleType, "./"
				+ XSD_RESTRICTION + XMLConstants.S_SLASH + XSD_MINEXCLUSIVE, XPATH_XSD);
		if (minEx.size() > 0) {
			Element elem = (Element) minEx.get(0);
			String value = elem.getAttributeValue("value");
			if (iMinExclusive == Integer.MIN_VALUE
					&& XSD_INTEGER.equals(javaType)) {
				try {
					iMinExclusive = Integer.parseInt(value);
				} catch (NumberFormatException e) {
					throw new RuntimeException("Bad length " + e);
				}
			} else if (Double.isNaN(dMinExclusive)
					&& XSD_DOUBLE.equals(javaType)) {
				try {
					dMinExclusive = (Util.parseFlexibleDouble(value));
				} catch (NumberFormatException e) {
					throw new RuntimeException("Bad length " + e);
				} catch (ParseException e) {
					throw new RuntimeException("Bad value for a double: "
							+ value, e);
				}
			}
		}

		List<Node> maxEx = XMLUtil.getQueryNodes(simpleType, "./"
				+ XSD_RESTRICTION + XMLConstants.S_SLASH + XSD_MAXEXCLUSIVE, XPATH_XSD);
		if (maxEx.size() > 0) {
			Element elem = (Element) maxEx.get(0);
			String value = elem.getAttributeValue("value");
			if (iMaxExclusive == Integer.MAX_VALUE
					&& XSD_INTEGER.equals(javaType)) {
				try {
					iMaxExclusive = Integer.parseInt(value);
				} catch (NumberFormatException e) {
					throw new RuntimeException("Bad length " + e);
				}
			} else if (Double.isNaN(dMaxExclusive)
					&& XSD_DOUBLE.equals(javaType)) {
				try {
					dMaxExclusive = (Util.parseFlexibleDouble(value));
				} catch (NumberFormatException e) {
					throw new RuntimeException("Bad length " + e);
				} catch (ParseException e) {
					throw new RuntimeException("Bad value for a double: "
							+ value, e);
				}
			}
		}

		List<Node> minInc = XMLUtil.getQueryNodes(simpleType, "./"
				+ XSD_RESTRICTION + XMLConstants.S_SLASH + XSD_MININCLUSIVE, XPATH_XSD);
		if (minInc.size() > 0) {
			Element elem = (Element) minInc.get(0);
			String value = elem.getAttributeValue("value");
			if (iMinInclusive == Integer.MIN_VALUE
					&& XSD_INTEGER.equals(javaType)) {
				try {
					iMinInclusive = Integer.parseInt(value);
				} catch (NumberFormatException e) {
					throw new RuntimeException("Bad length " + e);
				}
			} else if (Double.isNaN(dMinInclusive)
					&& XSD_DOUBLE.equals(javaType)) {
				try {
					dMinInclusive = (Util.parseFlexibleDouble(value));
				} catch (NumberFormatException e) {
					throw new RuntimeException("Bad length " + e);
				} catch (ParseException e) {
					throw new RuntimeException("Bad value for a double: "
							+ value, e);
				}
			}
		}

		List<Node> maxInc = XMLUtil.getQueryNodes(simpleType, "./"
				+ XSD_RESTRICTION + XMLConstants.S_SLASH + XSD_MAXINCLUSIVE, XPATH_XSD);
		if (maxInc.size() > 0) {
			Element elem = (Element) maxInc.get(0);
			String value = elem.getAttributeValue("value");
			if (iMaxInclusive == Integer.MAX_VALUE
					&& XSD_INTEGER.equals(javaType)) {
				try {
					iMaxInclusive = Integer.parseInt(value);
				} catch (NumberFormatException e) {
					throw new RuntimeException("Bad length " + e);
				}
			} else if (Double.isNaN(dMaxInclusive)
					&& XSD_DOUBLE.equals(javaType)) {
				try {
					dMaxInclusive = (Util.parseFlexibleDouble(value));
				} catch (NumberFormatException e) {
					throw new RuntimeException("Bad length " + e);
				} catch (ParseException e) {
					throw new RuntimeException("Bad value for a double: "
							+ value, e);
				}
			}
		}
	}

	void createEnumerations() {
		List<Node> restrictions = XMLUtil.getQueryNodes(simpleType, "./"
				+ XSD_RESTRICTION + XMLConstants.S_SLASH + XSD_ENUMERATION, XPATH_XSD);
		int size = restrictions.size();
		if (size > 0) {
			int i = 0;
			if (XSD_INTEGER.equals(javaType)) {
				iEnumerationValues = new int[size];
				for (Node node : restrictions) {
					Element restriction = (Element) node;
					try {
						iEnumerationValues[i++] = Integer.parseInt(restriction
								.getAttributeValue("value"));
					} catch (NumberFormatException nfe) {
						throw new RuntimeException(
								"Cannot parse enumeration as integer: " + nfe);
					}
				}
			} else if (XSD_DOUBLE.equals(javaType)) {
				dEnumerationValues = new double[size];
				for (Node node : restrictions) {
					Element restriction = (Element) node;
					try {
						dEnumerationValues[i++] = NumberFormat
								.getNumberInstance().parse(
										restriction.getAttributeValue("value"))
								.doubleValue();
					} catch (NumberFormatException nfe) {
						throw new RuntimeException(
								"Cannot parse enumeration as double: " + nfe);
					} catch (ParseException e) {
						throw new RuntimeException(
								"Bad value for a double: "
										+ restriction
												.getAttributeValue("value"), e);
					}
				}
			} else if (XSD_STRING.equals(javaType)) {
				sEnumerationValues = new String[size];
				for (Node node : restrictions) {
					Element restriction = (Element) node;
					sEnumerationValues[i++] = restriction
							.getAttributeValue("value");
				}
			}
		}
	}

	/**
	 * checks value of simpleType. throws CMLRuntime if value does not check
	 * against SimpleType or is a list currently only uses pattern. fails if
	 * type is int or double
	 * 
	 * @param s
	 *            the string
	 * @throws RuntimeException
	 *             wrong type or pattern fails
	 */
	public void checkValue(String s) throws RuntimeException {
		if (subTypes.length > 0) {
			for (int j = 0; j < subTypes.length; j++) {
				(subTypes[j]).checkValue(s);
			}
		} else {
			if (!base.equals(XSD_STRING)) {
				throw new RuntimeException("Cannot accept String for type: "
						+ base);
			}
			if (isList) {
				throw new RuntimeException(
						"cannot accept single String for String[] list");
			}
			checkPattern(s);
			checkEnumeration(s);
		}
	}

	/**
	 * checks value of simpleType. throws CMLRuntime if value does not check
	 * against SimpleType or is not a list currently only uses pattern. fails if
	 * type is int or double
	 * 
	 * @param ss
	 *            the strings
	 * @throws RuntimeException
	 *             wrong type or pattern fails
	 */
	public void checkValue(String ss[]) throws RuntimeException {
		if (subTypes.length > 0) {
			for (int j = 0; j < subTypes.length; j++) {
				(subTypes[j]).checkValue(ss);
			}
		} else {
			if (!base.equals(XSD_STRING)) {
				throw new RuntimeException("Cannot accept String for type: "
						+ base);
			}
			if (!isList) {
				throw new RuntimeException(
						"cannot accept a list String[] for single String");
			}
			checkListLength(ss.length);
			int i = 0;
			try {
				while (i < ss.length) {
					checkPattern(ss[i]);
					checkEnumeration(ss[i]);
					i++;
				}
			} catch (RuntimeException e) {
				throw new RuntimeException("String (" + i + ")(" + ss[i]
						+ ") fails: " + e);
			}
		}
	}

	private void checkListLength(int l) throws RuntimeException {
		// in many cases there is no set list length...
		// assume negative list length implies this?
		// if (listLength < Integer.MAX_VALUE && listLength != l) {
		if (listLength > 0 && listLength != l) {
			throw new RuntimeException("listLength required (" + listLength
					+ ") incompatible with: " + l);
		}
	}

	/**
	 * checks value of simpleType. throws CMLRuntime if value does not check
	 * against SimpleType or is a list currently uses min/max/In/Exclusive fails
	 * if type is String or double
	 * 
	 * @param i
	 *            the int
	 * @throws RuntimeException
	 *             wrong type or value fails
	 */
	public void checkValue(int i) throws RuntimeException {
		if (subTypes.length > 0) {
			for (int j = 0; j < subTypes.length; j++) {
				(subTypes[j]).checkValue(i);
			}
		} else {
			if (!base.equals(XSD_INTEGER)) {
				throw new RuntimeException("Cannot accept int for type: "
						+ base);
			}
			if (isList) {
				throw new RuntimeException(
						"cannot accept single int for int[] list");
			}
			checkMinMax(i);
			checkEnumeration(i);
		}
	}

	/**
	 * checks value of simpleType. throws CMLRuntime if value does not check
	 * against SimpleType or is not a list currently uses min/max/In/Exclusive
	 * fails if type is String or double
	 * 
	 * @param ii
	 *            the int
	 * @throws RuntimeException
	 *             wrong type or value fails
	 */
	public void checkValue(int ii[]) throws RuntimeException {
		if (subTypes.length > 0) {
			for (int j = 0; j < subTypes.length; j++) {
				(subTypes[j]).checkValue(ii);
			}
		} else {
			if (!base.equals(XSD_INTEGER)) {
				throw new RuntimeException("Cannot accept int for type: "
						+ base);
			}
			if (!isList) {
				throw new RuntimeException(
						"cannot accept a list int[] for single int");
			}
			checkListLength(ii.length);
			int i = 0;
			try {
				while (i < ii.length) {
					checkMinMax(ii[i]);
					checkEnumeration(ii[i]);
					i++;
				}
			} catch (RuntimeException e) {
				throw new RuntimeException("int[] (" + i + ")(" + ii[i]
						+ ") fails: " + e);
			}
		}
	}

	/**
	 * checks value of simpleType. throws CMLRuntime if value does not check
	 * against SimpleType or is a list currently uses min/max/In/Exclusive fails
	 * if type is String or int
	 * 
	 * @param d
	 *            the double
	 * @throws RuntimeException
	 *             wrong type or value fails
	 */
	public void checkValue(double d) throws RuntimeException {
		if (subTypes.length > 0) {
			for (int j = 0; j < subTypes.length; j++) {
				(subTypes[j]).checkValue(d);
			}
		} else {
			if (!base.equals(XSD_DOUBLE)) {
				throw new RuntimeException("Cannot accept double for type: "
						+ base);
			}
			if (isList) {
				throw new RuntimeException(
						"cannot accept single double for double[] list");
			}
			checkMinMax(d);
			checkEnumeration(d);
		}
	}

	/**
	 * checks value of simpleType. throws CMLRuntime if value does not check
	 * against SimpleType or is not a list currently uses min/max/In/Exclusive
	 * fails if type is String or int
	 * 
	 * @param dd
	 *            the double
	 * @throws RuntimeException
	 *             wrong type or value fails
	 */
	public void checkValue(double dd[]) throws RuntimeException {
		if (subTypes.length > 0) {
			for (int j = 0; j < subTypes.length; j++) {
				(subTypes[j]).checkValue(dd);
			}
		} else {
			if (!base.equals(XSD_DOUBLE)) {
				throw new RuntimeException("Cannot accept String for type: "
						+ base);
			}
			if (!isList) {
				throw new RuntimeException(
						"cannot accept a list double[] for single double");
			}
			checkListLength(dd.length);
			int i = 0;
			try {
				while (i < dd.length) {
					checkMinMax(dd[i]);
					checkEnumeration(dd[i]);
					i++;
				}
			} catch (RuntimeException e) {
				throw new RuntimeException("double[] (" + i + ")(" + dd[i]
						+ ") fails: " + e);
			}
		}
	}

	/**
	 * checks value of simpleType. throws CMLRuntime if value does not check
	 * against SimpleType or is a list fails if type is not boolean
	 * 
	 * @param b
	 *            the boolean
	 * @throws RuntimeException
	 *             wrong type or value fails
	 */
	public void checkValue(boolean b) throws RuntimeException {
		if (subTypes.length > 0) {
			for (int j = 0; j < subTypes.length; j++) {
				(subTypes[j]).checkValue(b);
			}
		} else {
			if (!base.equals(XSD_BOOLEAN)) {
				throw new RuntimeException(
						"Cannot accept boolean for type: " + base);
			}
			if (isList) {
				throw new RuntimeException(
						"cannot accept single boolean for boolean[] list");
			}
		}
	}

	/**
	 * checks value of simpleType. throws CMLRuntime if value does not check
	 * against SimpleType or is not a list fails if type is not boolean
	 * 
	 * @param bb
	 *            the boolean array
	 * @throws RuntimeException
	 *             wrong type or value fails
	 */
	public void checkValue(boolean bb[]) throws RuntimeException {
		if (subTypes.length > 0) {
			for (int j = 0; j < subTypes.length; j++) {
				(subTypes[j]).checkValue(bb);
			}
		} else {
			if (!base.equals(XSD_BOOLEAN)) {
				throw new RuntimeException(
						"Cannot accept boolean for type: " + base);
			}
			if (!isList) {
				throw new RuntimeException(
						"cannot accept a list boolean[] for single boolean");
			}
			checkListLength(bb.length);
		}
	}

	/**
	 * get name.
	 * 
	 * @return name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * set name.
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * get Base.
	 * 
	 * @return base
	 */
	public String getBase() {
		return this.base;
	}

	/**
	 * set base.
	 * 
	 * @param base
	 */
	public void setBase(String base) {
		this.base = base;
	}

	/**
	 * set id.
	 * 
	 * @param id
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * returns whether ST uses a list.
	 * 
	 * @return true if list.
	 */
	public boolean getIsList() {
		return isList;
	}

	/**
	 * set is list.
	 * 
	 * @param b
	 */
	public void setIsList(boolean b) {
		this.isList = b;
	}

	/**
	 * set pattern.
	 * 
	 * @param p
	 */
	public void setPattern(String p) {
		this.pattern = p;
	}

	/**
	 * get pattern.
	 * 
	 * @return pattern
	 */
	public String getPattern() {
		return this.pattern;
	}

	/**
	 * get list length.
	 * 
	 * @return length
	 */
	public int getListLength() {
		return this.listLength;
	}

	/**
	 * set list length.
	 * 
	 * @param l
	 */
	public void setListLength(int l) {
		this.listLength = l;
	}

	/**
	 * set min inclusive.
	 * 
	 * @param i
	 */
	public void setMinInclusive(int i) {
		this.iMinInclusive = i;
	}

	/**
	 * set min exclusive.
	 * 
	 * @param i
	 */
	public void setMinExclusive(int i) {
		this.iMinExclusive = i;
	}

	/**
	 * set max inclusive.
	 * 
	 * @param i
	 */
	public void setMaxInclusive(int i) {
		this.iMaxInclusive = i;
	}

	/**
	 * set max exclusive.
	 * 
	 * @param i
	 */
	public void setMaxExclusive(int i) {
		this.iMaxExclusive = i;
	}

	/**
	 * set min exclusive.
	 * 
	 * @param d
	 */

	public void setMinInclusive(double d) {
		this.dMinInclusive = d;
	}

	/**
	 * set min exclusive.
	 * 
	 * @param d
	 */
	public void setMinExclusive(double d) {
		this.dMinExclusive = d;
	}

	/**
	 * set max inclusive.
	 * 
	 * @param d
	 */
	public void setMaxInclusive(double d) {
		this.dMaxInclusive = d;
	}

	/**
	 * set max exclusive.
	 * 
	 * @param d
	 */
	public void setMaxExclusive(double d) {
		this.dMaxExclusive = d;
	}

	/**
	 * get int min inclusive.
	 * 
	 * @return min
	 */
	public int getIntMinInclusive() {
		return this.iMinInclusive;
	}

	/**
	 * get int min exclusive.
	 * 
	 * @return min
	 */
	public int getIntMinExclusive() {
		return this.iMinExclusive;
	}

	/**
	 * get int max inclusive.
	 * 
	 * @return max
	 */
	public int getIntMaxInclusive() {
		return this.iMaxInclusive;
	}

	/**
	 * get int max exclusive.
	 * 
	 * @return int
	 */
	public int getIntMaxExclusive() {
		return this.iMaxExclusive;
	}

	/**
	 * get double min inclusive.
	 * 
	 * @return min
	 */
	public double getDoubleMinInclusive() {
		return this.dMinInclusive;
	}

	/**
	 * get double min exclusive.
	 * 
	 * @return min
	 */
	public double getDoubleMinExclusive() {
		return this.dMinExclusive;
	}

	/**
	 * get double max inclusive.
	 * 
	 * @return max
	 */
	public double getDoubleMaxInclusive() {
		return this.dMaxInclusive;
	}

	/**
	 * get double max exclusive.
	 * 
	 * @return max
	 */
	public double getDoubleMaxExclusive() {
		return this.dMaxExclusive;
	}

	/**
	 * set subtypes.
	 * 
	 * @param st
	 */
	public void setSubTypes(STMLType[] st) {
		this.subTypes = new STMLType[st.length];
		for (int i = 0; i < st.length; i++) {
			subTypes[i] = st[i];
		}
	}

	// protected Elements subTypes = null;

	/**
	 * set enumeration.
	 * 
	 * @param ss
	 */
	public void setEnumeration(String[] ss) {
		this.sEnumerationValues = ss;
	}

	/**
	 * set enumeration.
	 * 
	 * @param ii
	 */
	public void setEnumeration(int[] ii) {
		this.iEnumerationValues = ii;
	}

	/**
	 * set enumeration.
	 * 
	 * @param dd
	 */
	public void setEnumeration(double[] dd) {
		this.dEnumerationValues = dd;
	}

	/**
	 * get string enumeration.
	 * 
	 * @return enumeration
	 */
	public String[] getStringEnumeration() {
		return this.sEnumerationValues;
	}

	/**
	 * get int enumeration.
	 * 
	 * @return enumeration
	 */
	public int[] getIntEnumeration() {
		return this.iEnumerationValues;
	}

	/**
	 * get double enumeration.
	 * 
	 * @return enumeration
	 */
	public double[] getDoubleEnumeration() {
		return this.dEnumerationValues;
	}

	private void checkPattern(String s) throws RuntimeException {
		if (s == null) {
			throw new RuntimeException("Null strings not allowed");
		}
		if (pattern != null && !s.matches(pattern)) {
			throw new RuntimeException("String (" + s
					+ ") does not match pattern (" + pattern + ") for " + name);
		}
	}

	private void checkMinMax(int i) throws RuntimeException {
		if (iMinInclusive > Integer.MIN_VALUE && i < iMinInclusive) {
			throw new RuntimeException("int (" + i + ") less than "
					+ iMinInclusive);
		}
		if (iMaxInclusive < Integer.MAX_VALUE && i > iMaxInclusive) {
			throw new RuntimeException("int (" + i + ") greater than "
					+ iMaxInclusive);
		}
		if (iMinExclusive > Integer.MIN_VALUE && i <= iMinExclusive) {
			throw new RuntimeException("int (" + i + ") less than equals "
					+ iMinExclusive);
		}
		if (iMaxExclusive < Integer.MAX_VALUE && i >= iMaxExclusive) {
			throw new RuntimeException("int (" + i
					+ ") greater than equals " + iMaxExclusive);
		}
	}

	private void checkMinMax(double d) throws RuntimeException {
		if (!Double.isNaN(dMinInclusive) && d < dMinInclusive) {
			throw new RuntimeException("double (" + d + ") less than "
					+ dMinInclusive);
		}
		if (!Double.isNaN(dMaxInclusive) && d > dMaxInclusive) {
			throw new RuntimeException("double (" + d + ") greater than "
					+ dMaxInclusive);
		}
		if (!Double.isNaN(dMinExclusive) && d <= dMinExclusive) {
			throw new RuntimeException("double (" + d
					+ ") less than equals " + dMinExclusive);
		}
		if (!Double.isNaN(dMaxExclusive) && d >= dMaxExclusive) {
			throw new RuntimeException("double (" + d
					+ ") greater than equals " + dMaxExclusive);
		}
	}

	private void checkEnumeration(int i) throws RuntimeException {
		if (iEnumerationValues.length != 0) {
			boolean ok = false;
			for (int j = 0; j < iEnumerationValues.length; j++) {
				if (i == iEnumerationValues[j]) {
					ok = true;
					break;
				}
			}
			if (!ok) {
				throw new RuntimeException("int (" + i
						+ ") not contained in enumeration");
			}
		}
	}

	private void checkEnumeration(double d) throws RuntimeException {
		if (dEnumerationValues.length != 0) {
			boolean ok = false;
			for (int j = 0; j < dEnumerationValues.length; j++) {
				if (d == dEnumerationValues[j]) {
					ok = true;
					break;
				}
			}
			if (!ok) {
				throw new RuntimeException("double (" + d
						+ ") not contained in enumeration");
			}
		}
	}

	private void checkEnumeration(String s) throws RuntimeException {
		if (s == null) {
			throw new RuntimeException(
					"Null String cannot be checked against enumeration");
		}
		if (dEnumerationValues.length != 0) {
			boolean ok = false;
			for (int j = 0; j < sEnumerationValues.length; j++) {
				if (s.equals(sEnumerationValues[j])) {
					ok = true;
					break;
				}
			}
			if (!ok) {
				throw new RuntimeException("String (" + s
						+ ") not contained in enumeration");
			}
		}
	}

	/**
	 * compares cmlType. uses name only as we expect to have singleton CMLTypes
	 * null values of any component return -1
	 * 
	 * @param type
	 *            to compare
	 * @return 0 if all content is identical, -1 if this less than att, 1 if
	 *         greater value
	 * 
	 */
	public int compareTo(STMLType type) {
		return name.compareTo(type.name);
	}

	/**
	 * get summary.
	 * 
	 * @return summary
	 */
	public String getSummary() {
		return summary;
	}

	/**
	 * set summary.
	 * 
	 * @param s
	 */
	public void setSummary(String s) {
		summary = s.trim();
		if (summary.length() != 0 && !summary.endsWith(S_PERIOD)) {
			summary += XMLConstants.S_PERIOD;
		}
	}

	/**
	 * get description.
	 * 
	 * @return description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * get full description.
	 * 
	 * @return description
	 */
	public String getFullDescription() {
		String desc = "";
		if (subTypes.length > 0) {
			// desc = "**************** UNION ****************\n";
			for (STMLType subType : subTypes) {
				desc += "********SUBTYPE*********\n";
				desc += subType.getFullDescription();
			}
		} else {
			if (summary != null && !summary.trim().equals("")) {
				desc += "\n....  " + summary;
			}
			if (description != null && !description.equals("")) {
				desc += "\n_______________________________________\n"
						+ description
						+ "\n__________________________________________\n";
			}
			if (pattern != null) {
				desc += "\n Pattern: " + pattern;
			}
			if (listLength != Integer.MIN_VALUE) {
				desc += "\nLength: " + listLength;
			}
			boolean min = false;
			if (iMinInclusive != Integer.MIN_VALUE) {
				desc += "\nMinInclusive: " + iMinInclusive;
				min = true;
			}
			if (iMinExclusive != Integer.MIN_VALUE) {
				desc += "\nMinExclusive: " + iMinExclusive;
				min = true;
			}
			if (!Double.isNaN(dMinInclusive)) {
				desc += "\nMinInclusive: " + dMinInclusive;
				min = true;
			}
			if (!Double.isNaN(dMinExclusive)) {
				desc += "\nMinExclusive: " + dMinExclusive;
				min = true;
			}

			if (iMaxInclusive != Integer.MAX_VALUE) {
				desc += ((min) ? "" : "\n") + " MaxInclusive: " + iMaxInclusive;
			}
			if (iMaxExclusive != Integer.MAX_VALUE) {
				desc += ((min) ? "" : "\n") + " MaxExclusive: " + iMaxExclusive;
			}
			if (!Double.isNaN(dMaxInclusive)) {
				desc += ((min) ? "" : "\n") + " MaxInclusive: " + dMaxInclusive;
			}
			if (!Double.isNaN(dMaxExclusive)) {
				desc += ((min) ? "" : "\n") + " MaxExclusive: " + dMaxExclusive;
			}

			if (sEnumerationValues.length > 0) {
				desc += "\nPermitted String values:";
				for (int i = 0; i < sEnumerationValues.length; i++) {
					desc += "\n  " + sEnumerationValues[i];
				}
			}
			if (iEnumerationValues.length > 0) {
				desc += "\nPermitted integer values:";
				for (int i = 0; i < iEnumerationValues.length; i++) {
					desc += "\n  " + iEnumerationValues[i];
				}
			}
			if (dEnumerationValues.length > 0) {
				desc += "\nPermitted double values:";
				for (int i = 0; i < dEnumerationValues.length; i++) {
					desc += "\n  " + dEnumerationValues[i];
				}
			}
		}
		return desc;
	}

	/**
	 * set description.
	 * 
	 * @param d
	 */
	public void setDescription(String d) {
		description = d;
	}

	/**
	 * to string.
	 * 
	 * @return string
	 */
	public String toString() {
		String s = "Name: " + name + "\n";
		if (union != null) {
			s += ".....UNION: " + subTypes.length;
		} else {
			s += this.getFullDescription();
			s += "\n";
		}
		return s;
	}

	/**
	 * @return the dEnumerationValues
	 */
	public double[] getDEnumerationValues() {
		return dEnumerationValues;
	}

	/**
	 * @param enumerationValues
	 *            the dEnumerationValues to set
	 */
	public void setDEnumerationValues(double[] enumerationValues) {
		dEnumerationValues = enumerationValues;
	}

	/**
	 * @return the dMaxExclusive
	 */
	public double getDMaxExclusive() {
		return dMaxExclusive;
	}

	/**
	 * @param maxExclusive
	 *            the dMaxExclusive to set
	 */
	public void setDMaxExclusive(double maxExclusive) {
		dMaxExclusive = maxExclusive;
	}

	/**
	 * @return the dMaxInclusive
	 */
	public double getDMaxInclusive() {
		return dMaxInclusive;
	}

	/**
	 * @param maxInclusive
	 *            the dMaxInclusive to set
	 */
	public void setDMaxInclusive(double maxInclusive) {
		dMaxInclusive = maxInclusive;
	}

	/**
	 * @return the dMinExclusive
	 */
	public double getDMinExclusive() {
		return dMinExclusive;
	}

	/**
	 * @param minExclusive
	 *            the dMinExclusive to set
	 */
	public void setDMinExclusive(double minExclusive) {
		dMinExclusive = minExclusive;
	}

	/**
	 * @return the dMinInclusive
	 */
	public double getDMinInclusive() {
		return dMinInclusive;
	}

	/**
	 * @param minInclusive
	 *            the dMinInclusive to set
	 */
	public void setDMinInclusive(double minInclusive) {
		dMinInclusive = minInclusive;
	}

	/**
	 * @return the iEnumerationValues
	 */
	public int[] getIEnumerationValues() {
		return iEnumerationValues;
	}

	/**
	 * @param enumerationValues
	 *            the iEnumerationValues to set
	 */
	public void setIEnumerationValues(int[] enumerationValues) {
		iEnumerationValues = enumerationValues;
	}

	/**
	 * @return the iMaxExclusive
	 */
	public int getIMaxExclusive() {
		return iMaxExclusive;
	}

	/**
	 * @param maxExclusive
	 *            the iMaxExclusive to set
	 */
	public void setIMaxExclusive(int maxExclusive) {
		iMaxExclusive = maxExclusive;
	}

	/**
	 * @return the iMaxInclusive
	 */
	public int getIMaxInclusive() {
		return iMaxInclusive;
	}

	/**
	 * @param maxInclusive
	 *            the iMaxInclusive to set
	 */
	public void setIMaxInclusive(int maxInclusive) {
		iMaxInclusive = maxInclusive;
	}

	/**
	 * @return the iMinExclusive
	 */
	public int getIMinExclusive() {
		return iMinExclusive;
	}

	/**
	 * @param minExclusive
	 *            the iMinExclusive to set
	 */
	public void setIMinExclusive(int minExclusive) {
		iMinExclusive = minExclusive;
	}

	/**
	 * @return the iMinInclusive
	 */
	public int getIMinInclusive() {
		return iMinInclusive;
	}

	/**
	 * @param minInclusive
	 *            the iMinInclusive to set
	 */
	public void setIMinInclusive(int minInclusive) {
		iMinInclusive = minInclusive;
	}

	/**
	 * @return the javaType
	 */
	public String getJavaType() {
		return javaType;
	}

	/**
	 * @param javaType
	 *            the javaType to set
	 */
	public void setJavaType(String javaType) {
		this.javaType = javaType;
	}

	/**
	 * @return the list
	 */
	public Element getList() {
		return list;
	}

	/**
	 * @param list
	 *            the list to set
	 */
	public void setList(Element list) {
		this.list = list;
	}

	/**
	 * @return the restriction
	 */
	public Element getRestriction() {
		return restriction;
	}

	/**
	 * @param restriction
	 *            the restriction to set
	 */
	public void setRestriction(Element restriction) {
		this.restriction = restriction;
	}

	/**
	 * @return the sEnumerationValues
	 */
	public String[] getSEnumerationValues() {
		return sEnumerationValues;
	}

	/**
	 * @param enumerationValues
	 *            the sEnumerationValues to set
	 */
	public void setSEnumerationValues(String[] enumerationValues) {
		sEnumerationValues = enumerationValues;
	}

	/**
	 * @return the simpleType
	 */
	public Element getSimpleType() {
		return simpleType;
	}

	/**
	 * @param simpleType
	 *            the simpleType to set
	 */
	public void setSimpleType(Element simpleType) {
		this.simpleType = simpleType;
	}

	/**
	 * @return the union
	 */
	public Element getUnion() {
		return union;
	}

	/**
	 * @param union
	 *            the union to set
	 */
	public void setUnion(Element union) {
		this.union = union;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the subTypes
	 */
	public STMLType[] getSubTypes() {
		return subTypes;
	}

	/**
	 * @param isList
	 *            the isList to set
	 */
	public void setList(boolean isList) {
		this.isList = isList;
	}

	/**
	 * get data type of list.
	 * 
	 * @return type
	 */
	public String listDataType() {
		String s = " ... base " + this.base;
		s += " (java: " + javaType + ") ";
		if (isList) {
			s += " [" + ((this.listLength >= 0) ? this.listLength : "*") + "]";
		}
		return s;
	}

	protected void removeAttributeWithName(String name) {
	}

	/**
	 * maps datatypes onto simpler values. mainly maps float, real, etc. to
	 * XSD_FLOAT
	 * 
	 * @param value
	 * @return normalized value
	 */
	public static String getNormalizedValue(String value) {
		String dataType = null;
		if (value == null || value.trim().equals("")
				|| value.equals(XSD_STRING)) {
			dataType = XSD_STRING;
		} else {
			value = value.trim();
			if (value.equals(XSD_INTEGER)) {
				dataType = XSD_INTEGER;
			} else if (value.equals(XSD_FLOAT) || value.equals(FPX_REAL)
					|| value.equals(XSD_DOUBLE)) {
				dataType = XSD_DOUBLE;
			} else if (value.equals(XSD_DATE)) {
				dataType = XSD_DATE;
			} else {
				throw new RuntimeException("Unknown data type: " + value);
			}
		}
		return dataType;
	}

}
