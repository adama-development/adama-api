package com.adama.api.service.excel.util;

import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;

public class FormattingHtml implements NodeVisitor {
	private StringBuilder accum = new StringBuilder();

	public void head(Node node, int depth) {
		String name = node.nodeName();
		if (node instanceof TextNode)
			append(((TextNode) node).text());
		else if (name.equals("li"))
			append("\n * ");
		else if (name.equals("dt"))
			append("  ");
		else if (StringUtil.in(name, "p", "h1", "h2", "h3", "h4", "h5", "tr"))
			append("\n");
	}

	public void tail(Node node, int depth) {
		String name = node.nodeName();
		if (StringUtil.in(name, "br", "dd", "dt", "p", "h1", "h2", "h3", "h4", "h5"))
			append("\n");
	}

	private void append(String text) {
		if (text.startsWith("\n"))
			if (text.equals(" ") && (accum.length() == 0 || StringUtil.in(accum.substring(accum.length() - 1), " ", "\n")))
				return;
		accum.append(text);
	}

	@Override
	public String toString() {
		return accum.toString();
	}
}