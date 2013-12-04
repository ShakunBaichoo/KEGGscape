package org.cytoscape.keggscape.internal.read.kgml;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.keggscape.internal.generated.Entry;
import org.cytoscape.keggscape.internal.generated.Graphics;
import org.cytoscape.keggscape.internal.generated.Pathway;
import org.cytoscape.keggscape.internal.generated.Product;
import org.cytoscape.keggscape.internal.generated.Reaction;
import org.cytoscape.keggscape.internal.generated.Relation;
import org.cytoscape.keggscape.internal.generated.Substrate;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;

public class KGMLMapper {
	private static final String NAME_DELIMITER = ", ";
	private static final String ID_DELIMITER = " ";
	
	// Default values
	private static final String MAP_COLOR = "#6999AE";
	private static final String TITLE_COLOR = "#32CCB6";
	
	private final Pathway pathway;
	private String pathwayIdString = null;
	
	private final CyNetwork network;
	
	private static final String KEGG_PATHWAY_ID = "KEGG_PATHWAY_ID";
	private static final String KEGG_PATHWAY_IMAGE = "KEGG_PATHWAY_IMAGE";
	private static final String KEGG_PATHWAY_LINK = "KEGG_PATHWAY_LINK";

	private static final String KEGG_NODE_X = "KEGG_NODE_X";
	private static final String KEGG_NODE_Y = "KEGG_NODE_Y";
	private static final String KEGG_NODE_WIDTH = "KEGG_NODE_WIDTH";
	private static final String KEGG_NODE_HEIGHT = "KEGG_NODE_HEIGHT";
	private static final String KEGG_NODE_LABEL = "KEGG_NODE_LABEL";
	private static final String KEGG_NODE_LABEL_LIST_FIRST = "KEGG_NODE_LABEL_LIST_FIRST";
	private static final String KEGG_NODE_LABEL_LIST = "KEGG_NODE_LABEL_LIST";
	private static final String KEGG_ID = "KEGG_ID";
	private static final String KEGG_NODE_LABEL_COLOR = "KEGG_NODE_LABEL_COLOR";
	private static final String KEGG_NODE_FILL_COLOR = "KEGG_NODE_FILL_COLOR";
	private static final String KEGG_NODE_REACTIONID = "KEGG_NODE_REACTIONID";
	
	private static final String KEGG_NODE_TYPE = "KEGG_NODE_TYPE";
	private static final String KEGG_NODE_SHAPE = "KEGG_NODE_SHAPE";

	private static final String KEGG_RELATION_TYPE = "KEGG_RELATION_TYPE";
	private static final String KEGG_REACTION_TYPE = "KEGG_REACTION_TYPE";
	private static final String KEGG_EDGE_COLOR = "KEGG_EDGE_COLOR";
	
	final String[] lightBlueMap = { "Other types of O-glycan biosynthesis",
			"Lipopolysaccharide biosynthesis",
			"Glycosaminoglycan biosynthesis - chondroitin sulfate / dermatan sulfate",
			"Glycosphingolipid biosynthesis - ganglio series",
			"Glycosphingolipid biosynthesis - globo series",
			"Glycosphingolipid biosynthesis - lacto and neolacto series",
			"Glycosylphosphatidylinositol(GPI)-anchor biosynthesis",
			"Glycosaminoglycan degradation",
			"Various types of N-glycan biosynthesis",
			"Glycosaminoglycan biosynthesis - keratan sulfate",
			"Mucin type O-Glycan biosynthesis",
			"N-Glycan biosynthesis",
			"Glycosaminoglycan biosynthesis - heparan sulfate / heparin",
			"Other glycan degradation"
	};
	final String[] lightBrownMap = { "Aminobenzoate degradation",
			"Atrazine degradation",
			"Benzoate degradation",
			"Bisphenol degradation",
			"Caprolactam degradation",
			"Chlorocyclohexane and chlorobenzene degradation",
			"DDT degradation",
			"Dioxin degradation",
			"Drug metabolism - cytochrome P450",
			"Drug metabolism - other enzymes",
			"Ethylbenzene degradation",
			"Fluorobenzoate degradation",
			"Metabolism of xenobiotics by cytochrome P450",
			"Naphthalene degradation",
			"Polycyclic aromatic hydrocarbon degradation",
			"Steroid degradation",
			"Styrene degradation",
			"Toluene degradation",
			"Xylene degradation"
	};

	final Map<String, CyNode> nodeMap = new HashMap<String, CyNode>();
	
	public KGMLMapper(final Pathway pathway, final CyNetwork network) {
		this.pathway = pathway;
		this.network = network;

		mapPathwayMetadata(pathway, network);
		
	}
	
	public void doMapping() {
		createKeggNodeTable();
		createKeggEdgeTable();
		if (pathway.getNumber().equals("01100")) {
			mapGlobalEntries();
			mapGlobalReactions();
		} else {
            mapEntries();
            mapRelations();
            mapReactions();
		}
	}
	
	private void createKeggEdgeTable() {
		network.getDefaultEdgeTable().createColumn(KEGG_RELATION_TYPE, String.class, true);
		network.getDefaultEdgeTable().createColumn(KEGG_REACTION_TYPE, String.class, true);
		network.getDefaultEdgeTable().createColumn(KEGG_EDGE_COLOR, String.class, true);
	}

	private void createKeggNodeTable() {
		network.getDefaultNodeTable().createColumn(KEGG_NODE_X, String.class, true);
		network.getDefaultNodeTable().createColumn(KEGG_NODE_Y, String.class, true);
		network.getDefaultNodeTable().createColumn(KEGG_NODE_WIDTH, String.class, true);
		network.getDefaultNodeTable().createColumn(KEGG_NODE_HEIGHT, String.class, true);
		network.getDefaultNodeTable().createColumn(KEGG_NODE_LABEL, String.class, true);
		network.getDefaultNodeTable().createColumn(KEGG_NODE_LABEL_LIST_FIRST, String.class, true);
		network.getDefaultNodeTable().createListColumn(KEGG_NODE_LABEL_LIST, String.class, true);
		network.getDefaultNodeTable().createListColumn(KEGG_ID, String.class, true);
		network.getDefaultNodeTable().createColumn(KEGG_NODE_LABEL_COLOR, String.class, true);
		network.getDefaultNodeTable().createColumn(KEGG_NODE_FILL_COLOR, String.class, true);
		network.getDefaultNodeTable().createColumn(KEGG_NODE_REACTIONID, String.class, true);
		network.getDefaultNodeTable().createColumn(KEGG_NODE_TYPE, String.class, true);
		network.getDefaultNodeTable().createColumn(KEGG_NODE_SHAPE, String.class, true);
	}

	private final void basicNodeMapping(final CyRow row, final Entry entry) {
			final Graphics graphics = entry.getGraphics().get(0);
			row.set(CyNetwork.NAME, entry.getId());
			row.set(KEGG_NODE_REACTIONID, entry.getReaction());
			row.set(KEGG_NODE_TYPE, entry.getType());
			mapIdList(entry.getName(), ID_DELIMITER, row, KEGG_ID);
			mapIdList(graphics.getName(), NAME_DELIMITER, row, KEGG_NODE_LABEL_LIST);
			row.set(KEGG_NODE_X, graphics.getX());
			row.set(KEGG_NODE_Y, graphics.getY());
			row.set(KEGG_NODE_WIDTH, graphics.getWidth());
			row.set(KEGG_NODE_HEIGHT, graphics.getHeight());
			row.set(KEGG_NODE_LABEL, graphics.getName());
			row.set(KEGG_NODE_SHAPE, graphics.getType());
	}

	private void mapEntries() {
		final List<Entry> entries = pathway.getEntry();
		
		for (final Entry entry : entries) {
			final CyNode cyNode = network.addNode();
			final CyRow row = network.getRow(cyNode); 
			basicNodeMapping(row, entry);
			row.set(KEGG_NODE_LABEL_COLOR, entry.getGraphics().get(0).getFgcolor());
			final String fillColor = entry.getGraphics().get(0).getBgcolor();
			
			if(entry.getGraphics().get(0).getName().startsWith("TITLE")) {
				row.set(KEGG_NODE_FILL_COLOR, TITLE_COLOR);
			} else if(entry.getType().equals("map")) {
				row.set(KEGG_NODE_FILL_COLOR, MAP_COLOR);
			} else {
				row.set(KEGG_NODE_FILL_COLOR, fillColor);
			}
			nodeMap.put(entry.getId(), cyNode);
		}
	}
	
	private void mapGlobalEntries() {
		final List<Entry> entries = pathway.getEntry();
		
		for (final Entry entry : entries) {
			final CyNode cyNode = network.addNode();
			final CyRow row = network.getRow(cyNode); 
			basicNodeMapping(row, entry);
			if (entry.getType().equals("map")
					&& Arrays.asList(lightBlueMap).contains(entry.getGraphics().get(0).getName())) {
				network.getRow(cyNode).set(KEGG_NODE_LABEL_COLOR, "#99CCFF");
				network.getRow(cyNode).set(KEGG_NODE_FILL_COLOR, "#FFFFFF");
			} else if (entry.getType().equals("map")
					&& Arrays.asList(lightBrownMap).contains(entry.getGraphics().get(0).getName())) {
				network.getRow(cyNode).set(KEGG_NODE_LABEL_COLOR, "#DA8E82");
				network.getRow(cyNode).set(KEGG_NODE_FILL_COLOR, "#FFFFFF");
			} else {
				network.getRow(cyNode).set(KEGG_NODE_LABEL_COLOR, entry.getGraphics().get(0).getFgcolor());
				network.getRow(cyNode).set(KEGG_NODE_FILL_COLOR, entry.getGraphics().get(0).getBgcolor());
			}
			nodeMap.put(entry.getId(), cyNode);			
		}
	}
	
	private void mapRelations() {
		final List<Relation> relations = pathway.getRelation();
		System.out.println(relations.size());
		for (final Relation relation : relations) {
			final CyNode sourceNode = nodeMap.get(relation.getEntry1());
			final CyNode targetNode = nodeMap.get(relation.getEntry2());
			final CyEdge newEdge = network.addEdge(sourceNode, targetNode, true);
			network.getRow(newEdge).set(KEGG_RELATION_TYPE, relation.getType());
		} 
	}
	
	private void mapReactions() {
		final List<Reaction> reactions = pathway.getReaction();
		System.out.println(reactions.size());
		for (Reaction reaction : reactions) {
			final CyNode reactionNode = nodeMap.get(reaction.getId());
			final List<Substrate> substrates = reaction.getSubstrate();
			for (final Substrate substrate : substrates) {
				final CyNode sourceNode = nodeMap.get(substrate.getId());
				final CyEdge newEdge = network.addEdge(sourceNode, reactionNode, true);
				network.getRow(newEdge).set(KEGG_REACTION_TYPE, reaction.getType());
			}
			final List<Product> products = reaction.getProduct();
			for (final Product product : products) {
				final CyNode targetNode = nodeMap.get(product.getId());
				final CyEdge newEdge = network.addEdge(reactionNode, targetNode, true);
				network.getRow(newEdge).set(KEGG_REACTION_TYPE, reaction.getType());
			}
		}
	}
	
	private void mapGlobalReactions() {
		final List<Reaction> reactions = pathway.getReaction();
		for (Reaction reaction : reactions) {
			final List<Substrate> substrates = reaction.getSubstrate();
			final List<Product> products = reaction.getProduct();
			for (Substrate substrate : substrates) {
				final CyNode substrateNode = nodeMap.get(substrate.getId());
				for (Product product : products) {
					final CyNode productNode = nodeMap.get(product.getId());
					final CyEdge newEdge = network.addEdge(substrateNode, productNode, true);
					mapReactionEdgeData(newEdge);
				}
			}
		}
	}
	
	private final void mapReactionEdgeData(CyEdge edge) {
		final CyNode source = edge.getSource();
		network.getRow(edge).set(KEGG_EDGE_COLOR, network.getRow(source).get(KEGG_NODE_FILL_COLOR, String.class));
	}

	
	private final void mapIdList(final String idListText, final String delimiter, final CyRow row, final String columnName) {
		final List<String> idList = new ArrayList<String>();
		final String[] ids = idListText.split(delimiter);
		for(String id: ids) {
			idList.add(id);
		}
		row.set(columnName, idList);
		if(ids.length != 0 && row.getTable().getColumn( columnName + "_FIRST") != null) {
			row.set(columnName + "_FIRST", ids[0]);
		}
	}
	
	private final void mapPathwayMetadata(final Pathway pathway, final CyNetwork network) {
		final String pathwayName = pathway.getName();
		final String linkToKegg = pathway.getLink();
		final String linkToImage = pathway.getImage();
		final String pathwayTitle = pathway.getTitle();
		this.pathwayIdString = pathway.getNumber();
		
		final CyRow networkRow = network.getRow(network);
		networkRow.set(CyNetwork.NAME, pathwayTitle);
		
		network.getDefaultNetworkTable().createColumn(KEGG_PATHWAY_ID, String.class, true);
		network.getDefaultNetworkTable().createColumn(KEGG_PATHWAY_IMAGE, String.class, true);
		network.getDefaultNetworkTable().createColumn(KEGG_PATHWAY_LINK, String.class, true);

		networkRow.set(KEGG_PATHWAY_LINK, linkToKegg);
		networkRow.set(KEGG_PATHWAY_IMAGE, linkToImage);
		networkRow.set(KEGG_PATHWAY_ID, pathwayName);
	}
	
	public String getPathwayId() {
		return pathwayIdString;
	}
}