package uk.co.cwspencer.gdb.messages;

import org.junit.Assert;
import org.junit.Test;
import uk.co.cwspencer.gdb.gdbmi.GdbMiMessage;
import uk.co.cwspencer.gdb.gdbmi.GdbMiParser;
import uk.co.cwspencer.gdb.gdbmi.GdbMiResultRecord;

import java.io.UnsupportedEncodingException;

/**
 * Tests for GdbMiMessageConverter.
 */
public class TestGdbMiMessageConverter
{
	/**
	 * Verifies the correct conversion of a 'connected' message.
	 */
	@Test
	public void testConnectedEvent() throws UnsupportedEncodingException
	{
		// Parse the message
		GdbMiParser parser = new GdbMiParser();
		String messageStr =
			"^connected,addr=\"0xfe00a300\",func=\"??\",args=[]\r\n" +
				"(gdb)\r\n";
		parser.process(messageStr.getBytes("US-ASCII"));
		GdbMiMessage message = parser.getMessages().get(0);

		// Convert the message
		GdbMiResultRecord record = (GdbMiResultRecord) message.records.get(0);
		Object object = GdbMiMessageConverter.processRecord(record);
		Assert.assertNotNull(object);
		Assert.assertTrue(object instanceof GdbConnectedEvent);

		GdbConnectedEvent connectedEvent = (GdbConnectedEvent) object;
		Assert.assertEquals(new Long(0xfe00a300l), connectedEvent.address);
		Assert.assertEquals("??", connectedEvent.function);
		Assert.assertTrue(connectedEvent.arguments.isEmpty());
	}

	/**
	 * Verifies the correct conversion of an 'error' message.
	 */
	@Test
	public void testErrorEvent() throws UnsupportedEncodingException
	{
		// Parse the message
		GdbMiParser parser = new GdbMiParser();
		String messageStr =
			"^error,msg=\"mi_cmd_exec_interrupt: Inferior not executing.\"\r\n" +
			"(gdb)\r\n";
		parser.process(messageStr.getBytes("US-ASCII"));
		GdbMiMessage message = parser.getMessages().get(0);

		// Convert the message
		GdbMiResultRecord record = (GdbMiResultRecord) message.records.get(0);
		Object object = GdbMiMessageConverter.processRecord(record);
		Assert.assertNotNull(object);
		Assert.assertTrue(object instanceof GdbErrorEvent);

		GdbErrorEvent errorEvent = (GdbErrorEvent) object;
		Assert.assertEquals("mi_cmd_exec_interrupt: Inferior not executing.", errorEvent.message);
	}

	/**
	 * Verifies the correct conversion of an 'exit' message.
	 */
	@Test
	public void testExitEvent() throws UnsupportedEncodingException
	{
		// Parse the message
		GdbMiParser parser = new GdbMiParser();
		String messageStr =
			"^exit\r\n";
		parser.process(messageStr.getBytes("US-ASCII"));
		GdbMiMessage message = parser.getMessages().get(0);

		// Convert the message
		GdbMiResultRecord record = (GdbMiResultRecord) message.records.get(0);
		Object object = GdbMiMessageConverter.processRecord(record);
		Assert.assertNotNull(object);
		Assert.assertTrue(object instanceof GdbExitEvent);
	}

	/**
	 * Verifies the correct conversion of a 'stopped' message.
	 */
	@Test
	public void testStoppedEvent() throws UnsupportedEncodingException
	{
		// Parse the message
		GdbMiParser parser = new GdbMiParser();
		String messageStr =
			"*stopped," +
			"reason=\"breakpoint-hit\"," +
			"disp=\"keep\"," +
			"bkptno=\"1\"," +
			"thread-id=\"0\"," +
			"frame={" +
				"addr=\"0x08048564\"," +
				"func=\"main\"," +
				"args=[{" +
					"name=\"argc\"," +
					"value=\"1\"}," +
					"{name=\"argv\"," +
					"value=\"0xbfc4d4d4\"}]," +
				"file=\"myprog.c\"," +
				"fullname=\"/home/nickrob/myprog.c\"," +
				"line=\"68\"}\r\n" +
				"(gdb)\r\n";
		parser.process(messageStr.getBytes("US-ASCII"));
		GdbMiMessage message = parser.getMessages().get(0);

		// Convert the message
		GdbMiResultRecord record = (GdbMiResultRecord) message.records.get(0);
		Object object = GdbMiMessageConverter.processRecord(record);
		Assert.assertNotNull(object);
		Assert.assertTrue(object instanceof GdbStoppedEvent);

		GdbStoppedEvent stoppedEvent = (GdbStoppedEvent) object;
		Assert.assertEquals(GdbStoppedEvent.Reason.BreakpointHit, stoppedEvent.reason);
		Assert.assertEquals(GdbStoppedEvent.BreakpointDisposition.Keep, stoppedEvent.breakpointDisposition);
		Assert.assertEquals(new Integer(1), stoppedEvent.breakpointNumber);
		Assert.assertEquals(new Integer(0), stoppedEvent.threadId);

		Assert.assertEquals(new Long(0x08048564), stoppedEvent.frame.address);
		Assert.assertEquals("main", stoppedEvent.frame.function);
		Assert.assertEquals("myprog.c", stoppedEvent.frame.file);
		Assert.assertEquals("/home/nickrob/myprog.c", stoppedEvent.frame.filePath);
		Assert.assertEquals(new Integer(68), stoppedEvent.frame.line);

		Assert.assertEquals(2, stoppedEvent.frame.arguments.size());
		Assert.assertEquals("1", stoppedEvent.frame.arguments.get("argc"));
		Assert.assertEquals("0xbfc4d4d4", stoppedEvent.frame.arguments.get("argv"));
	}
}
