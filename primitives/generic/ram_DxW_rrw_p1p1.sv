
module ram_DxW_rrw_p1p1 
    #(
        DEPTH       = 2048,
        WIDTH       = 8
    ) (
    	clock,

    	address_a,
    	wren_a,
    	data_a,
    	q_a,

    	address_b,
    	q_b
    );

    localparam DEPTH_BITS=$clog2(DEPTH);

	input	                    clock;
    
	input	[DEPTH_BITS-1:0]    address_a;
	input	                    wren_a;
	input	[WIDTH-1:0]         data_a;
	output	[WIDTH-1:0]         q_a;

	input	[DEPTH_BITS-1:0]    address_b;
	output	[WIDTH-1:0]         q_b;


    reg [WIDTH-1:0] ram0 [0:DEPTH-1];
    reg [WIDTH-1:0] q_a, q_b;

    always @(posedge clock) begin
        if (wren_a) begin
            ram0[address_a] <= data_a;
        end

        q_a <= ram0[address_a];
        q_b <= ram0[address_b];
    end
endmodule

