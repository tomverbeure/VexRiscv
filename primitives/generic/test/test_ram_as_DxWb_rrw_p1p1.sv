

module test_ram_as_DxWb_rrw_p1p1 (
	input           clock_a,
	input [31:0]    address_a,
	input           wren_a,
	input [7:0]     byteena_a,
	input [31:0]    data_a,
	output[31:0]    q_a,

	input           clock_b,
	input [31:0]    address_b,
	output[31:0]    q_b
);

    localparam DEPTH = 4096;
    localparam WIDTH = 16;

    localparam DEPTH_BITS = $clog2(DEPTH);
    localparam BE_BITS = WIDTH/8;

    ram_as_DxWb_rrw_p1p1 #(
        .DEPTH(DEPTH),
        .WIDTH(WIDTH)
    )
    u_ram ( 
        .clock_a(clock_a),
        .address_a(address_a[DEPTH_BITS-1:0]),
        .wren_a(wren_a),
        .byteena_a(byteena_a[BE_BITS-1:0]),
        .data_a(data_a[WIDTH-1:0]),
        .q_a(q_a[WIDTH-1:0]),

        .clock_b(clock_b),
        .address_b(address_b[DEPTH_BITS-1:0]),
        .q_b(q_b[WIDTH-1:0]) 
    );

endmodule
