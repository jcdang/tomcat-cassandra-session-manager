package io.dang.tomcat.servlet;
/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import util.HTMLFilter;

/**
 * Example servlet showing request headers
 *
 * @author James Duncan Davidson <duncan@eng.sun.com>
 */

public class SessionExample extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        out.println("<!DOCTYPE html><html>");
        out.println("<head>");
        out.println("<meta charset=\"UTF-8\" />");
        out.println("<title>Session Example</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h3>Session Example</h3>");

        HttpSession session = request.getSession(true);
        out.println("Session Id " + session.getId());
        out.println("<br>");
        out.println("Session Created ");
        out.println(new Date(session.getCreationTime()) + "<br>");
        out.println("Session Last Accessed ");
        out.println(new Date(session.getLastAccessedTime()));

        String dataName = request.getParameter("dataname");
        String dataValue = request.getParameter("datavalue");
        if (dataName != null && dataValue != null) {
            session.setAttribute(dataName, dataValue);
        }

        out.println("<P>");
        out.println("Session Data<br>");
        Enumeration<String> names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            String value = session.getAttribute(name).toString();
            out.println(HTMLFilter.filter(name) + " = "
                    + HTMLFilter.filter(value) + "<br>");
        }

        out.println("<P>");
        out.print("<form action=\"");
        out.print(response.encodeURL("SessionExample"));
        out.print("\" ");
        out.println("method=POST>");
        out.println("Name of session Attribute:");
        out.println("<input type=text size=20 name=dataname>");
        out.println("<br>");
        out.println("Value of session Attribute:");
        out.println("<input type=text size=20 name=datavalue>");
        out.println("<br>");
        out.println("<input type=submit>");
        out.println("</form>");

        out.println("<P>GET based form:<br>");
        out.print("<form action=\"");
        out.print(response.encodeURL("SessionExample"));
        out.print("\" ");
        out.println("method=GET>");
        out.println("Name of session Attribute:");
        out.println("<input type=text size=20 name=dataname>");
        out.println("<br>");
        out.println("Value of session Attribute:");
        out.println("<input type=text size=20 name=datavalue>");
        out.println("<br>");
        out.println("<input type=submit>");
        out.println("</form>");

        out.print("<p><a href=\"");
        out.print(HTMLFilter.filter(response.encodeURL("SessionExample?dataname=foo&datavalue=bar")));
        out.println("\" >URL encoded </a>");

        out.println("</body>");
        out.println("</html>");
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        doGet(request, response);
    }

}