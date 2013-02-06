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
	@Test
	public void testStopEvent() throws UnsupportedEncodingException
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
		Assert.assertTrue(object instanceof GdbStopEvent);

		GdbStopEvent stopEvent = (GdbStopEvent) object;
		Assert.assertEquals(stopEvent.reason, GdbStopEvent.Reason.BreakpointHit);
		Assert.assertEquals(stopEvent.breakpointDisposition,
			GdbStopEvent.BreakpointDisposition.Keep);
		Assert.assertEquals(stopEvent.breakpointNumber, new Integer(1));
		Assert.assertEquals(stopEvent.threadId, new Integer(0));

		Assert.assertEquals(stopEvent.frame.address, new Long(0x08048564));
		Assert.assertEquals(stopEvent.frame.function, "main");
		//Assert.assertEquals(stopEvent.frame.arguments, ...);  TODO
		Assert.assertEquals(stopEvent.frame.file, "myprog.c");
		Assert.assertEquals(stopEvent.frame.filePath, "/home/nickrob/myprog.c");
		Assert.assertEquals(stopEvent.frame.line, new Integer(68));
	}
}
