// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.jdisc.http.filter;

import static org.testng.AssertJUnit.assertTrue;

import java.net.InetSocketAddress;
import java.net.URI;

import java.util.*;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.yahoo.jdisc.HeaderFields;
import com.yahoo.jdisc.test.TestDriver;

import com.yahoo.jdisc.http.Cookie;

import com.yahoo.jdisc.http.HttpHeaders;
import com.yahoo.jdisc.http.HttpRequest;
import com.yahoo.jdisc.http.HttpRequest.Version;

public class DiscFilterRequestTest {

	private static HttpRequest newRequest(URI uri, HttpRequest.Method method, HttpRequest.Version version) {
		InetSocketAddress address = new InetSocketAddress("example.yahoo.com", 69);
        TestDriver driver = TestDriver.newSimpleApplicationInstanceWithoutOsgi();
        driver.activateContainer(driver.newContainerBuilder());
        HttpRequest request = HttpRequest.newServerRequest(driver, uri, method, version, address);
        request.release();
        assertTrue(driver.close());
        return request;
    }

	@Test
	public void testRequestConstruction(){
		URI uri = URI.create("http://localhost:8080/test?param1=abc");
		HttpRequest httpReq = newRequest(uri, HttpRequest.Method.GET, HttpRequest.Version.HTTP_1_1);
		httpReq.headers().add(HttpHeaders.Names.CONTENT_TYPE, "text/html;charset=UTF-8");
		httpReq.headers().add("X-Custom-Header", "custom_header");
		List<Cookie> cookies = new ArrayList<Cookie>();
		cookies.add(new Cookie("XYZ", "value"));
		cookies.add(new Cookie("ABC", "value"));
		httpReq.encodeCookieHeader(cookies);
		DiscFilterRequest request = new JdiscFilterRequest(httpReq);
		Assert.assertSame(request.getParentRequest(),httpReq);
		Assert.assertEquals(request.getHeader("X-Custom-Header"),"custom_header");
		Assert.assertEquals(request.getHeader(HttpHeaders.Names.CONTENT_TYPE),"text/html;charset=UTF-8");

		List<Cookie> c = request.getCookies();
		Assert.assertNotNull(c);
		Assert.assertEquals(c.size(), 2);

		Assert.assertEquals(request.getParameter("param1"),"abc");
		Assert.assertNull(request.getParameter("param2"));
		Assert.assertEquals(request.getVersion(),Version.HTTP_1_1);
		Assert.assertEquals(request.getProtocol(),Version.HTTP_1_1.name());
		Assert.assertNull(request.getRequestedSessionId());
	}

	@Test
	public void testRequestConstruction2() {
		URI uri = URI.create("http://localhost:8080/test");
		HttpRequest httpReq = newRequest(uri, HttpRequest.Method.GET, HttpRequest.Version.HTTP_1_1);
		httpReq.headers().add("some-header", "some-value");
		DiscFilterRequest request = new JdiscFilterRequest(httpReq);

		request.addHeader("some-header", "some-value");
		String value = request.getUntreatedHeaders().get("some-header").get(0);
		Assert.assertEquals(value,"some-value");
	}

	@Test
	public void testRequestAttributes() {
		URI uri = URI.create("http://localhost:8080/test");
		HttpRequest httpReq = newRequest(uri, HttpRequest.Method.GET, HttpRequest.Version.HTTP_1_1);
		DiscFilterRequest request = new JdiscFilterRequest(httpReq);
		request.setAttribute("some_attr", "some_value");

		Assert.assertEquals(request.containsAttribute("some_attr"),true);

		Assert.assertEquals(request.getAttribute("some_attr"),"some_value");

	}

	@Test
	public void testGetAttributeNames() {
		URI uri = URI.create("http://localhost:8080/test");
		HttpRequest httpReq = newRequest(uri, HttpRequest.Method.GET, HttpRequest.Version.HTTP_1_1);
		DiscFilterRequest request = new JdiscFilterRequest(httpReq);
		request.setAttribute("some_attr_1", "some_value1");
		request.setAttribute("some_attr_2", "some_value2");

		Enumeration<String> e = request.getAttributeNames();
		List<String> attrList = Collections.list(e);
		Assert.assertEquals(2, attrList.size());
		Assert.assertEquals(attrList.contains("some_attr_1"), true);
		Assert.assertEquals(attrList.contains("some_attr_2"), true);

	}

	@Test
	public void testRemoveAttribute() {
		URI uri = URI.create("http://localhost:8080/test");
		HttpRequest httpReq = newRequest(uri, HttpRequest.Method.GET, HttpRequest.Version.HTTP_1_1);
		DiscFilterRequest request = new JdiscFilterRequest(httpReq);
        request.setAttribute("some_attr", "some_value");

		Assert.assertEquals(request.containsAttribute("some_attr"),true);

		request.removeAttribute("some_attr");

		Assert.assertEquals(request.containsAttribute("some_attr"),false);
	}

	@Test
	public void testGetIntHeader() {
		URI uri = URI.create("http://localhost:8080/test");
		HttpRequest httpReq = newRequest(uri, HttpRequest.Method.GET, HttpRequest.Version.HTTP_1_1);
		DiscFilterRequest request = new JdiscFilterRequest(httpReq);

		Assert.assertEquals(-1, request.getIntHeader("int_header"));

		request.addHeader("int_header", String.valueOf(5));

		Assert.assertEquals(5, request.getIntHeader("int_header"));
	}

	@Test
	public void testDateHeader() {
		URI uri = URI.create("http://localhost:8080/test");
		HttpRequest httpReq = newRequest(uri, HttpRequest.Method.GET, HttpRequest.Version.HTTP_1_1);
		DiscFilterRequest request = new JdiscFilterRequest(httpReq);


		Assert.assertEquals(-1, request.getDateHeader(HttpHeaders.Names.IF_MODIFIED_SINCE));

		request.addHeader(HttpHeaders.Names.IF_MODIFIED_SINCE, "Sat, 29 Oct 1994 19:43:31 GMT");

		Assert.assertEquals(783459811000L, request.getDateHeader(HttpHeaders.Names.IF_MODIFIED_SINCE));
	}

	@Test
	public void testParameterAPIsAsList() {
        URI uri = URI.create("http://example.yahoo.com:8080/test?param1=abc&param2=xyz&param2=pqr");
		HttpRequest httpReq = newRequest(uri, HttpRequest.Method.GET, HttpRequest.Version.HTTP_1_1);
		DiscFilterRequest request = new JdiscFilterRequest(httpReq);
        Assert.assertEquals(request.getParameter("param1"),"abc");

        List<String> values = request.getParameterValuesAsList("param2");
        Assert.assertEquals(values.get(0),"xyz");
        Assert.assertEquals(values.get(1),"pqr");

        List<String> paramNames = request.getParameterNamesAsList();
        Assert.assertEquals(paramNames.size(), 2);

	}

	@Test
	public void testParameterAPI(){
	    URI uri = URI.create("http://example.yahoo.com:8080/test?param1=abc&param2=xyz&param2=pqr");
        HttpRequest httpReq = newRequest(uri, HttpRequest.Method.GET, HttpRequest.Version.HTTP_1_1);
        DiscFilterRequest request = new JdiscFilterRequest(httpReq);
        Assert.assertEquals(request.getParameter("param1"),"abc");

        Enumeration<String> values = request.getParameterValues("param2");
        List<String> valuesList = Collections.list(values);
        Assert.assertEquals(valuesList.get(0),"xyz");
        Assert.assertEquals(valuesList.get(1),"pqr");

        Enumeration<String> paramNames = request.getParameterNames();
        List<String> paramNamesList = Collections.list(paramNames);
        Assert.assertEquals(paramNamesList.size(), 2);
	}

	@Test
	public void testGetHeaderNamesAsList() {
	    URI uri = URI.create("http://localhost:8080/test");
        HttpRequest httpReq = newRequest(uri, HttpRequest.Method.GET, HttpRequest.Version.HTTP_1_1);
        httpReq.headers().add(HttpHeaders.Names.CONTENT_TYPE, "multipart/form-data");
        httpReq.headers().add("header_1", "value1");
        httpReq.headers().add("header_2", "value2");
        DiscFilterRequest request = new JdiscFilterRequest(httpReq);

        Assert.assertEquals(request.getHeaderNamesAsList() instanceof List, true);
        Assert.assertEquals(request.getHeaderNamesAsList().size(), 3);
	}

	@Test
    public void testGetHeadersAsList() {
        URI uri = URI.create("http://localhost:8080/test");
        HttpRequest httpReq = newRequest(uri, HttpRequest.Method.GET, HttpRequest.Version.HTTP_1_1);
        DiscFilterRequest request = new JdiscFilterRequest(httpReq);

        Assert.assertEquals(request.getHeaderNamesAsList() instanceof List, true);
        Assert.assertEquals(request.getHeaderNamesAsList().size(), 0);

        httpReq.headers().add("header_1", "value1");
        httpReq.headers().add("header_1", "value2");

        Assert.assertEquals(request.getHeadersAsList("header_1").size(), 2);
    }

	@Test
	public void testIsMultipart() {

		URI uri = URI.create("http://localhost:8080/test");
		HttpRequest httpReq = newRequest(uri, HttpRequest.Method.GET, HttpRequest.Version.HTTP_1_1);
		httpReq.headers().add(HttpHeaders.Names.CONTENT_TYPE, "multipart/form-data");
		DiscFilterRequest request = new JdiscFilterRequest(httpReq);

		Assert.assertEquals(true,DiscFilterRequest.isMultipart(request));

		httpReq = newRequest(uri, HttpRequest.Method.GET, HttpRequest.Version.HTTP_1_1);
		httpReq.headers().add(HttpHeaders.Names.CONTENT_TYPE, "text/html;charset=UTF-8");
		request = new JdiscFilterRequest(httpReq);

		Assert.assertEquals(DiscFilterRequest.isMultipart(request),false);

		Assert.assertEquals(DiscFilterRequest.isMultipart(null),false);


		httpReq = newRequest(uri, HttpRequest.Method.GET, HttpRequest.Version.HTTP_1_1);
		request = new JdiscFilterRequest(httpReq);
		Assert.assertEquals(DiscFilterRequest.isMultipart(request),false);
     }

	@Test
	public void testGetRemotePortLocalPort() {

        URI uri = URI.create("http://example.yahoo.com:8080/test");
		HttpRequest httpReq = newRequest(uri, HttpRequest.Method.GET, HttpRequest.Version.HTTP_1_1);
		DiscFilterRequest request = new JdiscFilterRequest(httpReq);

        Assert.assertEquals(69, request.getRemotePort());
        Assert.assertEquals(8080, request.getLocalPort());

		if (request.getRemoteHost() != null) // if we have network
	        Assert.assertEquals("example.yahoo.com", request.getRemoteHost());

        request.setRemoteAddr("1.1.1.1");

        Assert.assertEquals("1.1.1.1",request.getRemoteAddr());
	}

	@Test
	public void testCharacterEncoding() throws Exception {
		URI uri = URI.create("http://example.yahoo.com:8080/test");
		HttpRequest httpReq = newRequest(uri, HttpRequest.Method.GET, HttpRequest.Version.HTTP_1_1);
		DiscFilterRequest request = new JdiscFilterRequest(httpReq);
		request.setHeaders(HttpHeaders.Names.CONTENT_TYPE, "text/html;charset=UTF-8");

		Assert.assertEquals(request.getCharacterEncoding(), "UTF-8");

		httpReq = newRequest(uri, HttpRequest.Method.GET, HttpRequest.Version.HTTP_1_1);
		request = new JdiscFilterRequest(httpReq);
		request.setHeaders(HttpHeaders.Names.CONTENT_TYPE, "text/html");
		request.setCharacterEncoding("UTF-8");

		Assert.assertEquals(request.getCharacterEncoding(),"UTF-8");

		Assert.assertEquals(request.getHeader(HttpHeaders.Names.CONTENT_TYPE),"text/html;charset=UTF-8");
	}

	@Test
	public void testSetScheme() throws Exception {
		URI uri = URI.create("https://example.yahoo.com:8080/test");
		HttpRequest httpReq = newRequest(uri, HttpRequest.Method.GET, HttpRequest.Version.HTTP_1_1);
		DiscFilterRequest request = new JdiscFilterRequest(httpReq);

		request.setScheme("http", true);
		System.out.println(request.getUri().toString());
		Assert.assertEquals(request.getUri().toString(), "http://example.yahoo.com:8080/test");
	}

	@Test
	public void testGetServerPort() throws Exception {
		URI uri = URI.create("http://example.yahoo.com/test");
		HttpRequest httpReq = newRequest(uri, HttpRequest.Method.GET, HttpRequest.Version.HTTP_1_1);
		DiscFilterRequest request = new JdiscFilterRequest(httpReq);
		Assert.assertEquals(request.getServerPort(), 80);

		request.setUri(URI.create("https://example.yahoo.com/test"));
		Assert.assertEquals(request.getServerPort(), 443);

	}

	@Test
	public void testIsSecure() throws Exception {
		URI uri = URI.create("http://example.yahoo.com/test");
		HttpRequest httpReq = newRequest(uri, HttpRequest.Method.GET, HttpRequest.Version.HTTP_1_1);
		DiscFilterRequest request = new JdiscFilterRequest(httpReq);
		Assert.assertEquals(request.isSecure(), false);

		request.setUri(URI.create("https://example.yahoo.com/test"));
		Assert.assertEquals(request.isSecure(), true);

	}

    @Test
    public void requireThatUnresolvableRemoteAddressesAreSupported() {
        URI uri = URI.create("http://doesnotresolve.zzz:8080/test");
        HttpRequest httpReq = newRequest(uri, HttpRequest.Method.GET, HttpRequest.Version.HTTP_1_1);
        DiscFilterRequest request = new JdiscFilterRequest(httpReq);
        Assert.assertNull(request.getLocalAddr());
    }

    @Test
    public void testGetUntreatedHeaders() {
	URI uri = URI.create("http://example.yahoo.com/test");
        HttpRequest httpReq = newRequest(uri, HttpRequest.Method.GET, HttpRequest.Version.HTTP_1_1);
        httpReq.headers().add("key1", "value1");
        httpReq.headers().add("key2", Arrays.asList("value1","value2"));

        DiscFilterRequest request = new JdiscFilterRequest(httpReq);
        HeaderFields headers = request.getUntreatedHeaders();
        Assert.assertEquals(headers.keySet().size(), 2);
        Assert.assertEquals(headers.get("key1").get(0), "value1" );
        Assert.assertEquals(headers.get("key2").get(0), "value1" );
        Assert.assertEquals(headers.get("key2").get(1), "value2" );
    }

	@Test
	public void testClearCookies() throws Exception {
		URI uri = URI.create("http://example.yahoo.com/test");
		HttpRequest httpReq = newRequest(uri, HttpRequest.Method.GET, HttpRequest.Version.HTTP_1_1);
		httpReq.headers().put(HttpHeaders.Names.COOKIE, "XYZ=value");
		DiscFilterRequest request = new JdiscFilterRequest(httpReq);
		request.clearCookies();
		Assert.assertNull(request.getHeader(HttpHeaders.Names.COOKIE));
	}

	@Test
	public void testGetWrapedCookies() throws Exception {
		URI uri = URI.create("http://example.yahoo.com/test");
		HttpRequest httpReq = newRequest(uri, HttpRequest.Method.GET, HttpRequest.Version.HTTP_1_1);
		httpReq.headers().put(HttpHeaders.Names.COOKIE, "XYZ=value");
		DiscFilterRequest request = new JdiscFilterRequest(httpReq);
		JDiscCookieWrapper[] wrappers = request.getWrappedCookies();
		Assert.assertEquals(wrappers.length ,1);
		Assert.assertEquals(wrappers[0].getName(), "XYZ");
		Assert.assertEquals(wrappers[0].getValue(), "value");
	}

	@Test
	public void testAddCookie() {
		URI uri = URI.create("http://example.yahoo.com/test");
		HttpRequest httpReq = newRequest(uri, HttpRequest.Method.GET, HttpRequest.Version.HTTP_1_1);
		DiscFilterRequest request = new JdiscFilterRequest(httpReq);
		request.addCookie(JDiscCookieWrapper.wrap(new Cookie("name", "value")));

		List<Cookie> cookies = request.getCookies();
		Assert.assertEquals(cookies.size(), 1);
		Assert.assertEquals(cookies.get(0).getName(), "name");
		Assert.assertEquals(cookies.get(0).getValue(), "value");
	}
}
