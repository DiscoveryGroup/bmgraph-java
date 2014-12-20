# BMGraph File Format

Biomine graph visualization utilities use the "bmgraph" format to
describe graphs. This format is also output by the
[Biomine Web interface](http://biomine.cs.helsinki.fi).  Typical file
name extension for BMGraph files is `bmg`.

Each BMGraph file is an ASCII compatible text file consisting of
lines.  Each line is terminated by an UNIX-style linefeed (`\n`). The
following types of lines are allowed:

1. Special nodes (e.g. query starting nodes, terminal nodes)
2. Edges
3. Comments (some with special meaning)
4. Empty lines

## Nodes

Nodes are given in the format `Type_DBID`. Node types begin with a
letter (A–Z) and are followed by an underscore. The DBID part can be
any string of ASCII letters, numbers and some punctuation
(e.g. periods and colons), but may not end in a colon or underscore.
Conventionally the DBID part is formatted as Database:ID, except for
group nodes which must not contain a colon (:) in the DBID part.  Each
instance of the same (case-sensitive) node identifier is considered to
refer to the same node.

Nodes can also have attributes given in special comments as key-value
pairs. See under Special comments and Attributes for details.

### Special Nodes

Special nodes, such as query starting and terminal nodes, are placed
at the top of the BMGraph file (before any edges). Such nodes are
simply given alone on an otherwise empty line. These nodes will be
considered "special" by most Biomine utilities, and will generally be
protected from deletion due to filtering and might be shown more
prominently in visualization than other nodes.

An example of three special nodes at the top of a BMGraph file:

    Phenotype_MIM:104300
    Gene_EntrezGene:5663
    Gene_EntrezGene:8081

Note that merely for use with BMVis visualization, these lines are not
required. Instead, `queryset=start` and `queryset=end` attribute
definitions may be applied to the starting and terminal nodes (if
any), respectively. See below for more on attribute definitions.

## Edges

Edges are specified by giving the two nodes connected by it, and a
linktype for the edge:

    Node_A Node_B linktype

Nodes are given in the node format described above, separated by a
single space, and are followed by a linktype consisting of ASCII
letters, numbers and punctuation, separated by a single space. In some
cases the linktype may be entirely omitted, but Biomine utilities will
always output some linktype for edges, defaulting to "+" (as being the
opposite of "-", which is also the reverse linktype of the empty
string).

Nodes forming the edge need not be introduced first as special nodes,
though they may be. Any previously unseen nodes are simply added to
the graph.

Edges can also have any number of attributes as key-value pairs. These
are separated from each other and from the linktype by spaces and are
given in the format `key=value`. See under Attributes for details.

The edge direction information is preserved and can be visualized.
Edge direction is indicated by the order of the nodes, so that the
edge is from the first node to the second. The linktype names also
reflect the direction, e.g. `Article_1234 Gene_5678 refers_to` would
be the reverse of `Gene_5678 Article_1234 referred_by` or
alternatively `Gene_5678 Arctile_1234 -refers_to`.

Linktype names are matched to their reverse equivalents according to:

1. Linktypes specified in special comments (see below for details)
2. Built-in reverse names (e.g. `refers_to` / `referred_by`)
3. Reverse names formed by prepending a minus `-` to the name
(e.g. `link` / `-link`)

Even multiple reversals are supported, so that `--refers_to` becomes
`refers_to`. This allows a very simple utility to reverse any edge by
prepending a minus, without regard to edge names.

The built-in reverse names are automatically converted to the
minus-notation by most Biomine utilities, which simplifies parsing
considerably. That is, `referred_by` is converted to
`-refers_to`. BMVis and other visualization utilities may still
display the "human readable" names regardless of what is in the file.

An example of an edge with a single attribute:

    Gene_EntrezGene:5663 Article_PubMed:8574969 -refers_to goodness=0.511154

## Comments and Empty Lines

Comments are lines beginning with a hash `#` character followed by any
sequence of printable ASCII characters other than a space and an
underscore (which may be special comments, see below). Comments are
ignored (but generally preserved) by Biomine utilities. An example:

    # This is a comment.

Empty lines are always ignored.

## Special Comments

Special comments are lines that begin with a hash, a single space and
an underscore, followed by a recognized special comment type name
(e.g. attributes). Different Biomine utilities may recognize different
special comments. Any utility not recognizing a special comment must
treat it as an ordinary comment.

The most important special comments are listed here, but individual
utilities may support their own for any purpose.

### Node Attributes

    # _attributes Type_DBID key=value ...

Node attributes for the node `Type_DBID` (which must already have
appeared in the BMGraph file, either as a special node or as part of
an edge).  Attributes are given as key-value pairs separated by
spaces. A node is not required to have any attributes, in which case
it also need not have an `_attributes` line. A single node may have
any number of `_attributes` lines.  If a single attribute key appears
multiple times, the last seen takes effect. Otherwise all new lines
for a single node simply add new keys on top of the existing
attributes.

An example of node attributes:

    # _attributes Gene_EntrezGene:5663 alias=Gene(PSEN1) color=green queryset=start

### Linknames

    # _reverse linkname reverse_linkname
    # _symmetric symmetric_linkname

Linknames and their reverse names may be defined using the two kinds
of special comments; `_reverse` and `_symmetric`. A `_reverse` comment
defines a linkname and its reverse, the `reverse_linkname`. A
`_symmetric` line defines a single linkname, which is considered
symmetric (i.e. its reverse is the same as the forward).

**Important:** These comments must appear before any edges in the
BMGraph file!

Linknames thus defined add on top of the built-in linknames, which
consist of the few most common linknames in the Biomine database. This
is required for historical compatibility with existing graphs. The
built-in linknames may be overridden by these comments.

An example of how some built-in linknames might be defined:

    # _reverse refers_to referred_by
    # _reverse codes_for coded_by
    # _reverse has_child has_parent
    # _reverse contains contained_by
    # _symmetric is_related_to
    # _symmetric interacts_with
    # _symmetric has_synonym
    # _symmetric overlaps
    # _symmetric is_homologous_to

If built-in symmetric linknames are used in a BMGraph file, most
Biomine utilities will add the relevant `# _symmetric` lines to the
output, in order to preserve these definitions along with the data.

### Node Groups

    # _group Group_ID number_of_members Type memberID1,memberID2,...

Biomine filters enable grouping of equivalent nodes to a single group
node. The information of the group's original member nodes is
preserved in `_group` comments. Group member nodes are formed by
concatenating the `Type` field with each `memberID` field,
e.g. `Type_memberID1`. Historically the fields were separated by
colons (`:`) instead of spaces. Current tools support the historical
variation as well, but always output the new space-separated group
lines, to avoid problems with colons appearing in node ids.

Note that many utilities do not recognize these groups as such,
instead of treating them as a single node. This may be desirable for
many cases (e.g.  simplifying computation that would affect all nodes
of the group similarly), but in other cases special provisions need to
be taken. When in doubt, it is recommended to ungroup all groups
before processing.

It is not recommended that new tools support this type of grouping. In
the future BMVis should support groups defined by edges,
e.g. `Group_g:1 Member_m:1 has_member`, which would allow easy and
compatible group construction and destruction for visualization.

### Canvas Size

    # _canvas top_left_x,top_left_y,bottom_right_x,bottom_right_y

Layout information can be embedded into the BMGraph file.  This is
done by means of `pos` attributes for nodes, specifying the x and y
coordinates at which the node should be placed. The extents of the
graph can obviously be discovered by finding the minimum and maximum
coordinates over all `pos` attributes. However, the `_canvas` special
comment can be used to store the extents of the "graph canvas" in one
place, usually near the top of the graph file. No program should rely
on this attribute being specified, or on all node coordinates lying
within the canvas. The intended usage is to give a starting point for
the graph size and shape, e.g. to force a subgraph on a canvas similar
in size to a larger graph.

When using Graphviz for layout, the canvas coordinates are extracted
from the `bb` graph attribute, and the node coordinates from `pos`
attributes.

## Attributes

Both nodes and edges can have attributes associated to them. These can
be used to pass any information to other programs supporting the
BMGraph format. Edge attributes are given on the same line as the
edge, node attributes in special comments (due to historical reasons
and ease of parsing).

Each key must have some non-empty value if specified. Keys may consist
of ASCII letters and numbers. Values may consist of any printable
ASCII characters, except whitespace. Some Biomine utilities support
spaces in values by substituting them for the plus `+` character,
e.g. `two+words` could be visualized as "two words". There is
currently no escape implemented to display a literal plus instead.

Some keys are commonly recognized by Biomine utilities, for example:

* `goodness`, `reliability`, `relevance`, `rarity`: Crawler results.
* `queryname`: Name of a node in query input, e.g. matching search
  term.
* `alias`: Human-readable name of the node.
* `queryset`: Membership in a nodeset in the query, e.g. starting
  nodes have `queryset=start` and terminal nodes have `queryset=end`).
* `source_db_name`, `source_db_version`: Database information.
* `url`: Hyperlink from node or an edge label, overrides the
  auto-generated links for nodes.
* `pos`: Position in layout as a pair of comma-separated coordinates.
* `fill`: BMVis node or edge label fill color, given as `R/G/B` where
  R, G, and B are integers between 0 and 255, inclusive, giving the
  red, green, and blue color components.
* `color`, `bgcolor`, `fontcolor`, `style`: Graphviz attributes,
  mostly obsolete.
* `shape`: Graphviz shape for node only, mostly obsolete.
* `arrowhead`: Graphviz style for edge arrowhead only, mostly obsolete.
* `label`: node or edge label for visualization.

## Deprecated: Graphviz Directives

Graphviz directives in BMGraph files are deprecated. The following
section describes historical behaviour only, but new versions of
BMGraph parsers may simply ignore these, or even produce an error.

Graphviz directives are given on lines prefixed with a percent sign %.
Everything following the percent sign is passed to GraphViz
literally. It is not guaranteed that Graphviz will always be used as
the back-end for visualization of BMGraph files.

Example:

    % node [ color=pink ];

# Example BMGraph File

    Phenotype_MIM:104300
    Gene_EntrezGene:5663
    
    # _symmetric interacts_with
    
    Protein_UniProt:P49768 Gene_EntrezGene:5663 -codes_for goodness=0.8
    Protein_UniProt:P49768 Article_PubMed:10631141 -refers_to goodness=0.4
    Phenotype_MIM:104300 Article_PubMed:10631141 refers_to goodness=0.5
    
    # _attributes Gene_EntrezGene:5663 alias=Gene(PSEN1) queryset=start
    # _attributes Phenotype_MIM:104300 alias=Phenotype(AD) queryset=end

# Trivia

Due to the BMGraph file format, it is possible to completely remove a
node and its associated edges from a graph by simply removing all
lines containing the node's id (e.g. `fgrep -v Gene_EntrezGene:5663`).
