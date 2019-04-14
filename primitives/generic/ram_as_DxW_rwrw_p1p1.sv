
module ram_as_DxW_rwrw_p1p1 
    #(
        DEPTH       = 512,
        WIDTH       = 8
    ) (
    	clock_a,

    	address_a,
    	wren_a,
    	data_a,
    	q_a,

    	clock_b,

    	address_b,
    	wren_b,
    	data_b,
    	q_b
    );

    localparam DEPTH_BITS=$clog2(DEPTH);

	input	                    clock_a;
    
	input	[DEPTH_BITS-1:0]    address_a;
	input	                    wren_a;
	input	[WIDTH-1:0]         data_a;
	output	[WIDTH-1:0]         q_a;

	input	                    clock_b;

	input	[DEPTH_BITS-1:0]    address_b;
	input	                    wren_b;
	input	[WIDTH-1:0]         data_b;
	output	[WIDTH-1:0]         q_b;


    reg [WIDTH-1:0] ram0 [0:DEPTH-1];
    reg [WIDTH-1:0] q_a, q_b;

    always @(posedge clock_a) begin
        q_a <= ram0[address_a];
        if (wren_a) begin
            ram0[address_a] <= data_a;
//            q_a             <= data_a;
        end
    end

    always @(posedge clock_b) begin
        q_b <= ram0[address_b];
        if (wren_b) begin
            ram0[address_b] <= data_b;
//            q_b             <= data_b;
        end
    end
endmodule

