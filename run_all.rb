#! /usr/bin/env ruby
require 'pp'
require 'open3'

def run_test(option_str)

    log = []

    total_ticks = -1

    Open3.popen3("sbt \"test:runMain coremark.CoreMarkSim #{option_str}\"") do |stdout, stderr, status, thread|

        while line = stderr.gets do
            puts line
            log << line

            if line =~ /Total ticks\s*:\s*(\d+)/
                total_ticks = $1
            end
        end
    end

    total_ticks
end


PIPE_EMW    = 0
PIPE_EM     = 1
PIPE_E      = 2

BP_NONE     = 0
BP_STATIC   = 1
BP_DYN      = 2
BP_DYN_TGT  = 3

OPT_Os      = 0
OPT_O2      = 1
OPT_O3      = 2

MUL_NONE    = 0
MUL_ITER    = 1
MUL_SIMPLE  = 2

DIV_NONE    = 0
DIV_ITER    = 1
DIV_DHRY    = 2

SHIFT_ITER      = 0
SHIFT_BAR_EXE   = 1
SHIFT_BAR_MEM   = 2

pipe5_medium_config = {
        "--Pipe"    => PIPE_EMW,
        "--BrE"     => 0,
        "--BP"      => BP_STATIC,
        "--Shft"    => SHIFT_BAR_EXE,
        "--Mul"     => MUL_NONE,
        "--Div"     => DIV_NONE,
    }
pipe5_medium_config_str = pipe5_medium_config.collect { |opt, val| "#{opt}=#{val}" }.join(" ")

pipe4_medium_config = {
        "--Pipe"    => PIPE_EM,
        "--BrE"     => 1,
        "--BP"      => BP_STATIC,
        "--Shft"    => SHIFT_BAR_EXE,
        "--Mul"     => MUL_NONE,
        "--Div"     => DIV_NONE,
    }
pipe4_medium_config_str = pipe4_medium_config.collect { |opt, val| "#{opt}=#{val}" }.join(" ")

pipe3_medium_config = {
        "--Pipe"    => PIPE_E,
        "--BrE"     => 1,
        "--BP"      => BP_STATIC,
        "--Shft"    => SHIFT_BAR_EXE,
        "--Mul"     => MUL_NONE,
        "--Div"     => DIV_NONE,
    }
pipe3_medium_config_str = pipe3_medium_config.collect { |opt, val| "#{opt}=#{val}" }.join(" ")


all_tests = [
    { "bypass_impact_tests" =>  [
        { "5-stage, no bypass"  => "--BypE=0 --BypM=0 --BypW=0 --BypWB=0 #{pipe5_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },
        { "5-stage, bypassE"    => "--BypE=1 --BypM=0 --BypW=0 --BypWB=0 #{pipe5_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },
        { "5-stage, bypassM"    => "--BypE=0 --BypM=1 --BypW=0 --BypWB=0 #{pipe5_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },
        { "5-stage, bypassW"    => "--BypE=0 --BypM=0 --BypW=1 --BypWB=0 #{pipe5_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },
        { "5-stage, bypassWB"   => "--BypE=0 --BypM=0 --BypW=0 --BypWB=1 #{pipe5_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },
        { "5-stage, bypassAll"  => "--BypE=1 --BypM=1 --BypW=1 --BypWB=1 #{pipe5_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },

        { "4-stage, no bypass"  => "--BypE=0 --BypM=0 --BypW=0 --BypWB=0 #{pipe4_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },
        { "4-stage, bypassE"    => "--BypE=1 --BypM=0 --BypW=0 --BypWB=0 #{pipe4_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },
        { "4-stage, bypassM"    => "--BypE=0 --BypM=1 --BypW=0 --BypWB=0 #{pipe4_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },
        { "4-stage, bypassWB"   => "--BypE=0 --BypM=0 --BypW=0 --BypWB=1 #{pipe4_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },
        { "4-stage, bypassAll"  => "--BypE=1 --BypM=1 --BypW=1 --BypWB=1 #{pipe4_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },

        { "3-stage, no bypass"  => "--BypE=0 --BypM=0 --BypW=0 --BypWB=0 #{pipe3_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },
        { "3-stage, bypassE"    => "--BypE=1 --BypM=0 --BypW=0 --BypWB=0 #{pipe3_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },
        { "3-stage, bypassWB"   => "--BypE=0 --BypM=0 --BypW=0 --BypWB=1 #{pipe3_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },
        { "3-stage, bypassAll"  => "--BypE=1 --BypM=1 --BypW=1 --BypWB=1 #{pipe3_medium_config_str} --Opt=#{OPT_O2} --Gcc=0" },
    ] },
]

all_tests.each do |test_set|
    test_set_name = test_set.keys.first

    puts "============================================================"
    puts test_set_name
    puts "============================================================"

    test_set[test_set_name].each do |config, config_str|
        config_name = config.keys.first
        config_option_str = config[config_name]

        puts config_name
        total_ticks = run_test(config_option_str)
        puts "%%%%%% #{config_name} : #{total_ticks} : #{config_option_str}"

        config[:total_ticks] = total_ticks
    end
end

pp all_tests

