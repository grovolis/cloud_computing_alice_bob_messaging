package org.ncl.cloudcomputing.common;

public enum MessageStatus {
	Alice_to_TTP(1),
	TTP_to_Bob(2),
	Bob_to_TTP(3),
	TTP_to_Bob_doc(4),
	TTP_to_Alice(5),
	Transaction_Terminate(6);
	
	private final Integer value;
	
    private MessageStatus(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}
