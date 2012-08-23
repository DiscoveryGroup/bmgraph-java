/*
 * Copyright 2012 University of Helsinki.
 * 
 * This file is part of bmgraph-java.
 *
 * bmgraph-java is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * bmgraph-java is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with bmgraph-java.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package biomine.bmgraph;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Database links generator and related tools for Biomine source databases.
 *
 * <p>Nodes in Biomine graphs originate from certain source databases, most
 * of which contain more data for the node than is stored in the Biomine
 * database. As such, hyperlinks (URLs) from Biomine nodes to the online
 * interfaces of the source databases come in handy for presenting the
 * user with more information than is available locally. This class
 * contains the information and tools for generating these links from
 * Biomine node types and ids.
 *
 * @author Kimmo Kulovesi
 */

public class DatabaseLinks {

    /**
     * The parameters for the default Biomine group viewer request.
     */

    public static final String GROUPVIEW_PARAMS = "?db=|TYPE|&list_uids=|DBID|"; 

    /**
     * The URL for the default Biomine group viewer.
     */

    public static final String GROUPVIEW_URL = 
                "http://biomine.cs.helsinki.fi/bm/links.cgi" + GROUPVIEW_PARAMS; 

    /**
     * An enumeration of URL types for the databases and online viewers.
     * After finding out the proper URLType for a given node, an actual
     * viewing URL for the node can be generated from the type with
     * getURL(). Using an URLType with nodes not matching the type will
     * generate incorrect URLs (silently, without warnings).
     *
     * <p>Some URLTypes support multiple nodes in the same URL, in which
     * case either the |ID| or |DBID| field can contain many
     * identifiers. For the generated multi-node URLs to be valid, all
     * nodes must match the same URLType.
     */

    public enum URLType {
        /**
         * PubMed articles (supports multiple).
         */
        PUBMED ("Article", "PubMed", ",", "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=pubmed&dopt=Summary&query_hl=1&list_uids=|ID|", true),
       /**
        * Uniprot.
        */
        UNIPROT (null, "UniProt", null, "http://www.uniprot.org/uniprot/|ID|&format=html"),
        /**
         * EntrezGene (supports multiple).
         */
        ENTREZGENE (null, "EntrezGene", ",", "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&cmd=Retrieve&list_uids=|ID|", true),
        /**
         * Homologene (supports multiple).
         */
        HOMOLOGENE (null, "HomoloGene", ",", "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=homologene&dopt=HomoloGene&list_uids=|ID|"),
        /**
         * PubChem Substance (supports multiple)
         */
        PUBCHEM (null, "PubChem", ",", "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=pcsubstance&list_uids=|ID|"),
        /**
         * EntrezProtein.
         */
        ENTREZPROTEIN (null, "EntrezProtein", null, "http://www.ncbi.nlm.nih.gov/entrez/viewer.fcgi?db=protein&val=|ID|"), // DEBUG: Does this support multi?
        /**
         * STRING.
         */
        STRING (null, "String", null, "http://string.embl.de/newstring_cgi/show_link_summary.pl?identifier=|ID|"), // DEBUG: Does this support multi?
        /**
         * OMIM allelic variants.
         */
        ALLELICVARIANT ("AllelicVariant", "MIM", null, "http://www.ncbi.nlm.nih.gov/entrez/dispomim.cgi?id=|OMIMID|&a=|AVID|", false, true),
        /**
         * OMIM.
         */
        MIM (null, "MIM", null, "http://www.ncbi.nlm.nih.gov/entrez/dispomim.cgi?id=|ID|"),
        /*
         * This URL supports multiple IDs, but requires extra click to
         * get to the actual data (there is no "short summary" URL):
        MIM (null, "MIM", ",", "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=omim&list_uids=|ID|"),
        */
        /**
         * GO.
         */
	GO (null, "GO", null, "http://www.godatabase.org/cgi-bin/amigo/go.cgi?action=query&view=query&search_constraint=terms&query=|ID|"),
        /**
         * InterPro.
         */
        INTERPRO (null, "InterPro", null, "http://www.ebi.ac.uk/interpro/DisplayIproEntry?ac=|ID|"),
        /**
         * MeSH terms.
         */
	MESH (null, "MeSH", null, "http://www.nlm.nih.gov/cgi/mesh/2005/MB_cgi?mode=&term=|ID|"),
        /**
         * HGNC.
         */
        HGNC (null, "HGNC", null, "http://www.genenames.org/data/hgnc_data.php?hgnc_id=|ID|"),
        /**
         * Enzyme.
         * Could also use: "http://www.genome.jp/dbget-bin/www_bget?ec:|ID|",
         * but this does not currently (July 2009) handle categories,
         * resulting in broken links (even within KEGG itself, on their
         * own website).
         */
        ENZYME ("Enzyme", "EC", null, "http://au.expasy.org/enzyme/|ID|"),
        /**
         * KEGG Pathway.
         */
        KEGG_PATHWAY ("Pathway", "KEGG", null, "http://www.genome.jp/dbget-bin/www_bget?path:|ID|"),
        /**
         * KEGG Drug.
         */
        KEGG_DRUG ("Drug", "KEGG", null, "http://www.genome.jp/dbget-bin/www_bget?dr:|ID|"),
        /**
         * KEGG Compound.
         */
        KEGG_COMPOUND ("Compound", "KEGG", null, "http://www.genome.jp/dbget-bin/www_bget?cpd:|ID|"),
        /**
         * KEGG Glycan.
         */
        KEGG_GLYCAN ("Glycan", "KEGG", null, "http://www.genome.jp/dbget-bin/www_bget?gl:|ID|"),
        /**
         * KEGG (catch the entries with no specific db search).
         */
        KEGG (null, "KEGG", null, "http://www.genome.jp/dbget-bin/www_bfind_sub?mode=bfind&max_hit=1000&dbkey=kegg&keywords=|ID|"),
        /**
         * KEGG/KO OrthologGroup.
         */
        KEGG_ORTHOLOG ("OrthologGroup", "KEGG/KO", null, "http://www.genome.jp/dbget-bin/www_bget?ko:|ID|"),
        /**
         * KEGG/KO (catch the entries with no specific db search).
         */
        KEGG_KO (null, "KEGG/KO", null, "http://www.genome.jp/dbget-bin/www_bfind_sub?mode=bfind&max_hit=1000&dbkey=kegg&keywords=|ID|"),
        /**
         * COG.
         */
        COG (null, "COG", null, "http://www.ncbi.nlm.nih.gov/COG/grace/wiew.cgi?|ID|"),
        /**
         * GoMapMan.
         */
        GOMAPMAN (null, "GoMapMan", null, "http://www.gomapman.org/list_ontology/|ID|"),
        /**
         * GoMapMan Gene.
         */
        GOMAPMAN_GENE (null, "GoMapMan/Gene", null, "http://www.gomapman.org/gene/|ID|"),
        /**
         * GoMapMan VirtualGene.
         */
        GOMAPMAN_VIRTUALGENE (null, "GoMapMan/VirtualGene", null, "http://www.gomapman.org/gene/|ID|"),
        /**
         * DBLP Article.
         */
        DBLP_ARTICLE ("Article", "DBLP/Article", null, "http://www.pubzone.org/dblp/|ID|"),
        /**
         * DBLP Author.
         */
        DBLP_AUTHOR ("Author", "DBLP/Author", null, "http://dblp.uni-trier.de/rec/pid/|ID|")
            ;

        private final String type, db;
        private final boolean directGroups;

        /**
         * Separator for multiple ids, or null if not supported.
         */
        public final String multi;

        /**
         * URL string. This may contain any the following strings to be
         * replaced with the appropriate values when URLs are being
         * generated: |ID| for id(s), |DBID| for database:id(s), |DB|
         * for database, and |TYPE| for node type. The correct separator
         * must be used for multiple ids (if supported).
         */
        public final String url;
        
        private boolean hasSpecialNeeds;

        /*
         * Constructor for URLTypes.
         * @param type Node type required to match, or null for any.
         * @param db Node database required to match, or null for any.
         * @param multi Multiple node separator in URL, or null if unsupported.
         * @param url URL String (with at least |ID| or |DBID| for node ids).
         * @param directGroups True if groups of nodes of this type
         * can be viewed directly with this URLType (default false).
         * @param specialNeeds True if this URLType is a special case
         * needing non-standard URL formation.
         */
        URLType (String type, String db, String multi, String url,
                 boolean directGroups, boolean specialNeeds) {
            this.type = type;
            this.db = db;
            this.multi = multi;
            this.url = url;
            this.hasSpecialNeeds = specialNeeds;
            if (multi == null) {
                if (directGroups) {
                    System.err.println("db="+db+" type="+type);
                    assert !directGroups : "directGroups without multi-support?";
                }
                this.directGroups = false;
            } else {
                this.directGroups = directGroups;
            }
        }

        URLType (String type, String db, String multi, String url,
                 boolean directGroups) {
            this(type, db, multi, url, directGroups, false);
        }

        URLType (String type, String db, String multi, String url) {
            this(type, db, multi, url, false, false);
        }

        /**
         * Does this URLType support multiple ids in one URL?
         * @return True iff multiple ids are supported in this URLType.
         */
        public boolean supportsMulti () {
            return (multi != null);
        }

        /**
         * Are direct links to the databases preferred for groups of
         * this type?
         * @return True iff direct groups are preferred over GROUPVIEW_URL.
         */
        public boolean preferDirectGroups () {
            return directGroups;
        }

        /**
         * Get the URL for a given node.
         * The given node is assumed to match this URLType, but that is
         * not checked. Invalid URLs will be generated for non-matching
         * nodes.
         * @param type Node type.
         * @param db Node database.
         * @param id Node id.
         * @return The URL.
         */
        public String getURL (String type, String db, String id) {
            if (type == null)
                type = this.type;
            if (db == null)
                db = this.db;
            if (type == null || db == null || id == null)
                return null;

            String result = url.replace("|DB|", db);
            result = result.replace("|TYPE|", type);
            result = result.replace("|ID|", id);
            result = result.replace("|DBID|", db+":"+id);

            if (hasSpecialNeeds) {
                if (type.equals("AllelicVariant")) {
                    String[] omims = id.split("[.]");
                    if (omims.length == 2) {
                        result = result.replace("|OMIMID|", omims[0]);
                        result = result.replace("|AVID|",
                                    omims[0] + "_AllelicVariant" + omims[1]);
                    }
                }
            }

            return result;
        }

        /**
         * Get the URL for a given node. Node type is set according to
         * the URLType, which is assumed to match the node.
         * @param db Node database.
         * @param id Node id in db.
         * @return The URL.
         */
        public String getURL (String db, String id) {
            return getURL(type, db, id);
        }

        /**
         * Get the URL for a given node. Node type and database are set
         * according to the URLType, which is assumed to match the node.
         * @param id Node id.
         * @return The URL.
         */
        public String getURL (String id) {
            return getURL(type, db, id);
        }

        /**
         * Get the URL for a given BMNode. The BMNode is assumed to
         * match the URLType.
         * @param node The BMNode.
         * @return The URL.
         */
        public String getURL (BMNode node) {
            assert node != null : "Null node";

            String type = node.getType();
            String[] dbid = node.splitId();
            if (dbid == null)
                return getURL(type, db, node.getId());
            return getURL(type, dbid[0], dbid[1]);
        }

        /**
         * Get the URL for a given list of BMNodes.
         * The nodes are assumed to ALL match the URLType, and the
         * URLType must support multiple nodes per URL (that is, the
         * method supportsMulti() must return true).
         * @param iter Iterator over the BMNodes.
         * @return The URL or null if no nodes or multiple nodes not supported,
         * or if the nodes are mismatching in type.
         */
        public String getURL (Iterator<BMNode> iter) {
            assert iter != null : "Null iterator";

            int comma;
            BMNode node;
            String result, dbid, memberdb, membertype;
            String ids = "", dbids = "";

            if (!supportsMulti() || !iter.hasNext())
                return null;

            node = iter.next();
            membertype = node.getType();
            result = url.replace("|TYPE|", membertype);
            if (type == null || !type.equals(membertype))
                membertype = null;

            dbid = node.getId();
            comma = dbid.indexOf(':');
            if (comma > 1) {
                memberdb = dbid.substring(0, comma);
                result = result.replace("|DB|", memberdb);
                if (db == null || !db.equals(memberdb))
                    memberdb = null;
                ids = dbid.substring(comma + 1);
            } else {
                ids = dbid;
                memberdb = null;
            }
            while (iter.hasNext()) {
                node = iter.next();
                if (membertype != null && !membertype.equals(node.getType()))
                    return null;
                dbid = node.getId();
                dbids = dbids + "," + dbid;
                comma = dbid.indexOf(':');
                if (comma > 1) {
                    if (memberdb != null &&
                        !memberdb.equals(dbid.substring(0, comma)))
                        return null;
                    ids = ids + "," + dbid.substring(comma + 1);
                } else if (memberdb != null) {
                    return null;
                } else {
                    ids = ids + "," + dbid;
                }
            }
            result = result.replace("|ID|", ids);
            return result.replace("|DBID|", dbids);
        }

    }

    /**
     * Get the URLType for a given node type and database.
     * @param type Node type.
     * @param db Node db.
     * @return A matching URLType or null if none.
     */
    public static URLType getURLType (String type, String db) {
        for (URLType ut : URLType.values()) {
            if (ut.type == null || ut.type.equalsIgnoreCase(type)) {
                if (ut.db == null || ut.db.equalsIgnoreCase(db)) {
                    return ut;
                }
            }
        }
        return null;
    }

    /**
     * Get the URLType for a given BMNode.
     * @param node The BMNode.
     * @return A matching URLType or null if none.
     */
    public static URLType getURLType (BMNode node) {
        assert node != null : "Null node";

        String dbid = node.getId();
        int comma = dbid.indexOf(':');
        if (comma < 1)
            return null;
        return getURLType(node.getType(), dbid.substring(0, comma));
    }

    /**
     * Get the URL for a given node type, database and id.
     * @param type Node type.
     * @param db Node database.
     * @param id Node id.
     * @return The URL or null if no matching URLType.
     */
    public static String getURL (String type, String db, String id) {
        URLType urlType = getURLType(type, db);
        if (urlType == null)
            return null;
        return urlType.getURL(type, db, id);
    }

    /**
     * Get the URL for a given BMNode.
     * @param node The BMNode.
     * @return The URL or null if no matching URLType.
     */
    public static String getURL (BMNode node) {
        assert node != null : "Null node";

        String type = node.getType();
        String[] dbid = node.splitId();
        if (dbid == null)
            return null;
        return getURL(type, dbid[0], dbid[1]);
    }

    /**
     * Get the URL for a given BMNode group. A URL is always returned
     * for valid BMNodes, but it may be invalid unless the nodes form
     * a valid Biomine node group present in the database. The nodes in
     * a group must be of matching types (that of the groupnode).
     * @param group The groupnode.
     * @param members The list of group membernodes.
     * @return The URL for the group or null if no members.
     */
    public static String getGroupURL (BMNode group, List<BMNode> members) {
        return getGroupURL(group, members, GROUPVIEW_URL);
    }

    /**
     * Get the URL for a given BMNode group using a specified viewer.
     * A URL is always returned for valid BMNodes, but it may be invalid
     * unless the nodes form a valid Biomine node group present in
     * the database. The nodes in a group must be of matching types
     * (that of the groupnode).
     * @param group The groupnode.
     * @param members The list of group membernodes.
     * @param url The URL template, a string in which |TYPE| is replaced
     * by the nodetype and |DBID| by the comma-separated Database:ID list.
     * May be null, in which case default URL for the Biomine project is used.
     * @return The URL for the group or null if no members.
     */
    public static String getGroupURL (BMNode group, List<BMNode> members,
                                      String url) {
        assert group != null : "Null group";
        if (members == null) {
            members = new LinkedList<BMNode>();
            members.add(group);
        }

        if (url == null || url.length() < 1)
            url = GROUPVIEW_URL;

        String result;
        Iterator<BMNode> iter = members.iterator();
        if (!iter.hasNext())
            return null;
        BMNode member = iter.next();

        if (member != group) {
            URLType urlType = getURLType(member);
            if (urlType != null && urlType.preferDirectGroups()) {
                result = urlType.getURL(members.iterator());
                if (result != null)
                    return result;
            }
        }

        result = url.replace("|TYPE|", group.getType());
        String dbids = member.getId();
        while (iter.hasNext()) {
            member = iter.next();
            dbids = dbids + "," + member.getId();
        }
        return result.replace("|DBID|", dbids);
    }

    /**
     * Get a list of URLs for a set of BMNodes. The nodes must contain
     * no group nodes, as they cannot be handled here without knowledge
     * of the group members (use GetGroupURL instead). If the nodes are
     * presented in sorted order (first by type, then by database),
     * advantage can be taken of URLTypes supporting multiple nodes per
     * URL.
     * @param nodes Iterator for BMNodes.
     * @param viewURL An optional URL to view any node without a specific URL.
     * @return LinkedList containing node URLs as Strings.
     */

    public static LinkedList<String> getURLs (Iterator<BMNode> nodes,
                                              String viewURL) {
        LinkedList<BMNode> multi = new LinkedList<BMNode>();
        LinkedList<String> urls = new LinkedList<String>();
        URLType type;
        URLType prevType = null;
        String url;
        BMNode node;

        while (nodes.hasNext()) {
            node = nodes.next();
            url = node.get("url"); // DEBUG: Hard-coded constant "url"
            if (url != null) {
                urls.addLast(url);
                continue;
            }

            type = getURLType(node);
            if (type == null) {
                if (viewURL != null) {
                    // Use generic viewer if provided
                    url = getGroupURL(node, null, viewURL);
                    if (url != null) {
                        urls.addLast(url);
                    }
                }
                continue;
            }
            if (!multi.isEmpty()) {
                if (prevType == type) {
                    multi.addLast(node);
                    continue;
                } else {
                    urls.addLast(prevType.getURL(multi.iterator()));
                    multi.clear();
                }
            }
            if (type.supportsMulti()) {
                multi.addLast(node);
                prevType = type;
            } else {
                prevType = null;
                urls.addLast(type.getURL(node));
            }
        }
        if (!multi.isEmpty())
            urls.addLast(prevType.getURL(multi.iterator()));

        return urls;
    }

    /**
     * Get a list of URLs for a set of BMNodes. This is the same
     * as the two-parameter version, but viewURL is null.
     * @param nodes Iterator for BMNodes.
     * @return LinkedList containing node URLs as Strings.
     */

    public static LinkedList<String> getURLs (Iterator<BMNode> nodes) {
        return getURLs(nodes, null);
    }

    /**
     * Print URL(s) for the node(s) given as arguments.
     * @param args List of nodes as "type_db:id".
     */

    public static void main (String[] args) {
        LinkedList<BMNode> nodes = new LinkedList<BMNode>();
        LinkedList<String> urls;

        for (String node : args) {
            try {
                nodes.add(new BMNode(node));
            } catch (Exception e) {
                System.err.println("Error: Invalid node: "+node);
            }
        }
        urls = getURLs(nodes.iterator());
        for (String url : urls) {
            System.out.println(url);
        }
    }

}
