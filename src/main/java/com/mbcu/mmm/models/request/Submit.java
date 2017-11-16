package com.mbcu.mmm.models.request;

public class Submit extends Request {

	String tx_blob;

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getTx_blob() {
		return tx_blob;
	}

	public void setTx_blob(String tx_blob) {
		this.tx_blob = tx_blob;
	}

	public Submit(Command command) {
		super(command);
	}

	public static Submit build(String tx_blob) {
		Submit submit = new Submit(Request.Command.SUBMIT);
		submit.setTx_blob(tx_blob);
		return submit;
	}

	@Override
	public String stringify() {
		return super.stringify(this);
	}

}
