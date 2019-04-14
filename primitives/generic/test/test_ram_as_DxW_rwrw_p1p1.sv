

module test_ram_as_DxW_rwrw_p1p1 (
	input           clock_a,
	input [31:0]    address_a,
	input           wren_a,
	input [31:0]    data_a,
	output[31:0]    q_a,

	input           clock_b,
	input [31:0]    address_b,
	input           wren_b,
	input [31:0]    data_b,
	output[31:0]    q_b
);

    localparam DEPTH = 2048;
    localparam WIDTH = 8;

    localparam DEPTH_BITS = $clog2(DEPTH);

    ram_as_DxW_rwrw_p1p1 #(
        .DEPTH(DEPTH),
        .WIDTH(WIDTH)
    )
    u_ram ( 
        .clock_a(clock_a),
        .address_a(address_a[DEPTH_BITS-1:0]),
        .wren_a(wren_a),
        .data_a(data_a[WIDTH-1:0]),
        .q_a(q_a[WIDTH-1:0]),

        .clock_b(clock_b),
        .address_b(address_b[DEPTH_BITS-1:0]),
        .wren_b(wren_b),
        .data_b(data_b[WIDTH-1:0]),
        .q_b(q_b[WIDTH-1:0]) 
    );

endmodule
