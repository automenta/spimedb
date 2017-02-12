function BubblesFacetSVG(f, result) {
    const svg = d3.select(f[0]),
        width = +svg.attr("width"),
        height = +svg.attr("height");

    var format = d3.format(",d");

    var color = d3.scaleOrdinal(d3.schemeCategory20c);

    var pack = d3.pack()
        .size([width, height])
        .padding(1.5);


    const root = d3.hierarchy({children: result})
        .sum(function (d) {
            return d[1];
        })
        .each(function (d) {
            d.id = d.data[0];
            d.value = Math.log(1 + d.data[1]);
        });

    const node = svg.selectAll(".node")
        .data(pack(root).leaves())
        .enter().append("g")
        .attr("class", "node")
        .attr("transform", function (d) {
            return "translate(" + d.x + "," + d.y + ")";
        });

    node.append("circle")
        .attr("id", function (d) {
            return d.id;
        })
        .attr("r", function (d) {
            return d.r;
        })
        .style("fill", function (d) {
            return color(d.id);
        });

    node.append("clipPath")
        .attr("id", function (d) {
            return "clip-" + d.id;
        })
        .append("use")
        .attr("xlink:href", function (d) {
            return "#" + d.id;
        });

    node.append("text")
        .attr("clip-path", function (d) {
            return "url(#clip-" + d.id + ")";
        })
        .selectAll("tspan")
        .data(function (d) {
            return d.id.split(/(?=[A-Z][^A-Z])/g);
        })
        .enter().append("tspan")
        .attr("x", 0)
        .attr("y", function (d, i, nodes) {
            return 13 + (i - nodes.length / 2 - 0.5) * 10;
        })
        .text(function (d) {
            return d;
        });

    node.append("title")
        .text(function (d) {
            return d.id;// + "\n" + format(d.value);
        });
}
