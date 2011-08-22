<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:xmi="http://schema.omg.org/spec/XMI/2.1"
xmlns:uml="http://www.eclipse.org/uml2/3.0.0/UML">
<xsl:output method="xml" indent="yes"/>

<xsl:template match="/">
	<xsl:element name="machines">
		<xsl:apply-templates/>
	</xsl:element>
</xsl:template>

<xsl:template match="uml:Package/packagedElement[@xmi:type='uml:Activity']">
	<xsl:element name="machine">
		<xsl:attribute name="name">
			<xsl:value-of select="@name"/>
		</xsl:attribute>
		<xsl:apply-templates select="node[@xmi:type='uml:OpaqueAction']"/>
	</xsl:element>
</xsl:template>

<!-- node is actually State under machine -->
<xsl:template match="node">
	<xsl:element name="state">
		<xsl:attribute name="name">
			<xsl:value-of select="@name"/>
		</xsl:attribute>	
		<xsl:apply-templates select="body"/>
	</xsl:element>
</xsl:template>

<!-- body have two section either start with if or label -->
<xsl:template match="body">
	<xsl:variable name="text"><xsl:value-of select="."/></xsl:variable>
	<xsl:choose>
		<xsl:when test="contains($text,'label')">
			<xsl:element name="labels">
				<xsl:call-template name="getLabel">
					<xsl:with-param name="string"><xsl:value-of select="substring-after($text,'label')"/></xsl:with-param>
				</xsl:call-template>
			</xsl:element>
		</xsl:when>
		<xsl:when test="contains($text,'p[')">
			<xsl:element name="decisionTree">
				<xsl:value-of select="$text"/>
			</xsl:element>
		</xsl:when>
		<xsl:otherwise></xsl:otherwise>
	</xsl:choose>
</xsl:template>

<xsl:template name="getLabel">
	<xsl:param name="string"/>
	<xsl:variable name="subString_"/>
		<xsl:element name="labelPair">
			<xsl:attribute name="name">
				<xsl:variable name="subString_"><xsl:value-of select="substring-before($string,'=')"/>
				</xsl:variable>					
				<xsl:value-of select="$subString_"/>
			</xsl:attribute>
			<xsl:element name="instance">
				<xsl:variable name="instance_"><xsl:value-of select="substring-after($string,'=')"></xsl:value-of></xsl:variable>
				<xsl:value-of select="substring-before($instance_,';')"></xsl:value-of>
			</xsl:element>
		</xsl:element>
		<xsl:choose>
			<xsl:when test="contains($string, 'label')">
				<xsl:call-template name="getLabel">
					<xsl:with-param name="string"><xsl:value-of select="substring-after($string,'label')"/></xsl:with-param>
				</xsl:call-template>	
			</xsl:when>
		</xsl:choose>

</xsl:template>


</xsl:stylesheet>