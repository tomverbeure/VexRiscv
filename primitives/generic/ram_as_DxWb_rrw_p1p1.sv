
module ram_as_DxWb_rrw_p1p1 
    #(
        DEPTH       = 2048,
        WIDTH       = 8
    ) (
    	input                   clock_a,

    	input [DEPTH_BITS-1:0]  address_a,
    	input                   wren_a,
        input [BE_BITS-1:0]     byteena_a,
    	input [WIDTH-1:0]       data_a,
    	output reg [WIDTH-1:0]  q_a,

    	input                   clock_b,

    	input [DEPTH_BITS-1:0]  address_b,
    	output reg [WIDTH-1:0]  q_b
    );

    localparam DEPTH_BITS=$clog2(DEPTH);
    localparam BE_BITS=WIDTH/8;

    genvar i;
    generate for(i=0;i<BE_BITS;i=i+1) begin : loop
        reg [WIDTH/BE_BITS-1:0] ram [0:DEPTH-1];

        always @(posedge clock_a) begin
            if (wren_a && byteena_a[i]) begin
                ram[address_a] <= data_a[(i+1)*8-1:i*8];
            end

            q_a[(i+1)*8-1:i*8] <= ram[address_a];
        end

        always @(posedge clock_b) begin
            q_b[(i+1)*8-1:i*8] <= ram[address_b];
        end
    end
    endgenerate

endmodule

