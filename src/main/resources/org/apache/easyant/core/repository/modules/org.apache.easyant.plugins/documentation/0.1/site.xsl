<?xml version="1.0" encoding="ISO-8859-1"?>
<!--
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->
<!-- Content Stylesheet for "project-site" Documentation -->


<!-- $Id$ -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="1.0">


  <!-- Output method -->
  <xsl:output method="xml"
              encoding="iso-8859-1"
              doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
              doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
              indent="yes"/>


  <!-- Defined parameters (overrideable) -->
  <xsl:param    name="relative-path" select="'.'"/>
  <xsl:param    name="menu-src-path" select="'.'"/>
  <!-- Process an entire document into an HTML page -->
  <xsl:template match="document">
	  <xsl:variable name="filename" select="concat($menu-src-path, '/menu.xml')"/>  
    <xsl:variable name="project"
	    select="document($filename)/project"/>

    <xsl:variable name="homelink" select="project/@href"/>
    <xsl:variable name="title" select="$project/title"/>
    <xsl:variable name="logo" select="$project/logo"/>

    <html>
    <head>
        <xsl:apply-templates select="meta"/>
        <title><xsl:value-of select="$project/title"/> - <xsl:value-of select="properties/title"/></title>

        <xsl:for-each select="properties/author">
          <xsl:variable name="name">
            <xsl:value-of select="."/>
          </xsl:variable>
          <xsl:variable name="email">
            <xsl:value-of select="@email"/>
          </xsl:variable>
          <meta name="author" value="{$name}"/>
          <meta name="email" value="{$email}"/>
        </xsl:for-each>
        <xsl:if test="properties/base">
          <base href="{properties/base/@href}"/>
  </xsl:if>

        <style type="text/css" media="all">
			@import url("style.css");
        </style>
        <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
    </head>

    <body>
        <div id="body">
        <div id="inner_body">
            <div id="frame_logo">
                <div class="logo">
				    <a href="{$homelink}" 
				       title="{$title}">
				       <img src="{$logo}" alt="{$title}" border="0" />
				    </a>
                </div>

                <div class="title">
                    <h1><xsl:value-of select="$title"/></h1>
                </div>

            </div>
          
            <div id="content_wrapper">
                <div id="content_wrapper" class="nav_left">
                    <xsl:apply-templates select="$project/body/menu"/>
                </div>
                <div id="content_wrapper" class="content">
                    <xsl:apply-templates select="body/section"/>
                </div>
            </div>
                
			<div id="footer">
                <em>Copyright &#169; 1999-2008, The Apache Software Foundation</em>
            </div>
		</div>	
		</div>	
  </body>
  </html>
  </xsl:template>


  <!-- Process a menu for the navigation bar -->
  <xsl:template match="menu">
        <h1><xsl:value-of select="@name"/></h1>
        <ul>
            <xsl:apply-templates select="item"/>
        </ul>
  </xsl:template>


  <!-- Process a menu item for the navigation bar -->
  <xsl:template match="item">
    <xsl:variable name="href">
      <xsl:choose>
            <xsl:when test="starts-with(@href, 'http://')">
                <xsl:value-of select="@href"/>
            </xsl:when>
            <xsl:when test="starts-with(@href, 'https://')">
                <xsl:value-of select="@href"/>
            </xsl:when>
            <xsl:when test="starts-with(@href, '/')">
                <xsl:value-of select="@href"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$relative-path"/><xsl:value-of select="@href"/>
            </xsl:otherwise>    
      </xsl:choose>
    </xsl:variable>
    <li>
        <a href="{$href}" class="menu_item"><xsl:value-of select="@name"/></a>
    </li>
  </xsl:template>


  <!-- Process a documentation section -->
  <xsl:template match="section">
    <xsl:variable name="name">
      <xsl:value-of select="@name"/>
    </xsl:variable>
    <h1 id="section_heading">
          <a name="{$name}"><xsl:value-of select="@name"/></a>
    </h1>
      <!-- Section body -->
      <p><blockquote>
        <xsl:apply-templates/>
      </blockquote></p>
  </xsl:template>


  <!-- Process a documentation subsection -->
  <xsl:template match="subsection">
    <xsl:variable name="name">
      <xsl:value-of select="@name"/>
    </xsl:variable>
    <h2 id="subsection_heading">
          <a name="{$name}"><xsl:value-of select="@name"/></a>
      </h2>
      <!-- Subsection body -->
        <blockquote>
        <xsl:apply-templates/>
      </blockquote>
  </xsl:template>


  <!-- Process a source code example -->
  <xsl:template match="source">
    <div align="left">
      <table>
        <tr>
          <td>
            <img src="/images/void.gif" width="1" height="1" vspace="0" hspace="0" border="0"/>
          </td>
          <td bgcolor="grey" height="1">
            <img src="/images/void.gif" width="1" height="1" vspace="0" hspace="0" border="0"/>
          </td>
          <td bgcolor="grey" width="1" height="1">
            <img src="/images/void.gif" width="1" height="1" vspace="0" hspace="0" border="0"/>
          </td>
        </tr>
        <tr>
          <td bgcolor="grey" width="1">
            <img src="/images/void.gif" width="1" height="1" vspace="0" hspace="0" border="0"/>
          </td>
          <td bgcolor="#ffffff" height="1"><pre>
            <xsl:value-of select="."/>
          </pre></td>
          <td bgcolor="grey" width="1">
            <img src="/images/void.gif" width="1" height="1" vspace="0" hspace="0" border="0"/>
          </td>
        </tr>
        <tr>
          <td bgcolor="grey" width="1" height="1">
            <img src="/images/void.gif" width="1" height="1" vspace="0" hspace="0" border="0"/>
          </td>
          <td bgcolor="grey" height="1">
            <img src="/images/void.gif" width="1" height="1" vspace="0" hspace="0" border="0"/>
          </td>
          <td bgcolor="grey" width="1" height="1">
            <img src="/images/void.gif" width="1" height="1" vspace="0" hspace="0" border="0"/>
          </td>
        </tr>
      </table>
    </div>
  </xsl:template>

  <!-- specially process td tags ala site.vsl -->
  <xsl:template match="table[@class='detail-table']/tr/td">
    <td class="detail-table">
        <xsl:if test="@colspan"><xsl:attribute name="colspan"><xsl:value-of select="@colspan"/></xsl:attribute></xsl:if>
        <xsl:if test="@rowspan"><xsl:attribute name="rowspan"><xsl:value-of select="@rowspan"/></xsl:attribute></xsl:if>
        <xsl:apply-templates/>
    </td>
  </xsl:template>
  
  <!-- handle th ala site.vsl -->
  <xsl:template match="table[@class='detail-table']/tr/th">
    <td class="detail-table-header">
        <xsl:if test="@colspan"><xsl:attribute name="colspan"><xsl:value-of select="@colspan"/></xsl:attribute></xsl:if>
        <xsl:if test="@rowspan"><xsl:attribute name="rowspan"><xsl:value-of select="@rowspan"/></xsl:attribute></xsl:if>
        <xsl:apply-templates />
    </td>
  </xsl:template>
  
  <!-- Process everything else by just passing it through -->
  <xsl:template match="*|@*">
    <xsl:copy>
      <xsl:apply-templates select="@*|*|text()"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
