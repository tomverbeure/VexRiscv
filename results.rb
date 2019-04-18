RESULTS = [{"bypass_impact_tests"=>
   [{"6-stage M2"=>
      [{"no bypass"=>
         "--BypE=0 --BypM=0 --BypM2=0 --BypW=0 --BypWB=0 --Pipe=3 --BrE=0 --BP=1 --Shft=1 --Mul=0 --Div=0 --Opt=1 --Gcc=0",
        :result=>{"total_ticks"=>"2221796"}},
       {"bypassE"=>
         "--BypE=1 --BypM=0 --BypM2=0 --BypW=0 --BypWB=0 --Pipe=3 --BrE=0 --BP=1 --Shft=1 --Mul=0 --Div=0 --Opt=1 --Gcc=0",
        :result=>{"total_ticks"=>"1787993"}},
       {"bypassM"=>
         "--BypE=0 --BypM=1 --BypM2=0 --BypW=0 --BypWB=0 --Pipe=3 --BrE=0 --BP=1 --Shft=1 --Mul=0 --Div=0 --Opt=1 --Gcc=0",
        :result=>{"total_ticks"=>"1617389"}},
       {"bypassM2"=>
         "--BypE=0 --BypM=0 --BypM2=1 --BypW=0 --BypWB=0 --Pipe=3 --BrE=0 --BP=1 --Shft=1 --Mul=0 --Div=0 --Opt=1 --Gcc=0",
        :result=>{"total_ticks"=>"1683031"}},
       {"bypassW"=>
         "--BypE=0 --BypM=0 --BypM2=0 --BypW=1 --BypWB=0 --Pipe=3 --BrE=0 --BP=1 --Shft=1 --Mul=0 --Div=0 --Opt=1 --Gcc=0",
        :result=>{"total_ticks"=>"1763938"}},
       {"bypassWB"=>
         "--BypE=0 --BypM=0 --BypM2=0 --BypW=0 --BypWB=1 --Pipe=0 --BrE=0 --BP=1 --Shft=1 --Mul=0 --Div=0 --Opt=1 --Gcc=0",
        :result=>{"total_ticks"=>"1751119"}},
       {"bypassAll"=>
         "--BypE=1 --BypM=1 --BypM2=1 --BypW=1 --BypWB=1 --Pipe=3 --BrE=0 --BP=1 --Shft=1 --Mul=0 --Div=0 --Opt=1 --Gcc=0",
        :result=>{"total_ticks"=>"1285183"}}]},
    {"5-stage"=>
      [{"no bypass"=>
         "--BypE=0 --BypM=0           --BypW=0 --BypWB=0 --Pipe=0 --BrE=0 --BP=1 --Shft=1 --Mul=0 --Div=0 --Opt=1 --Gcc=0",
        :result=>{"total_ticks"=>"1985288"}},
       {"bypassE"=>
         "--BypE=1 --BypM=0           --BypW=0 --BypWB=0 --Pipe=0 --BrE=0 --BP=1 --Shft=1 --Mul=0 --Div=0 --Opt=1 --Gcc=0",
        :result=>{"total_ticks"=>"1635460"}},
       {"bypassM"=>
         "--BypE=0 --BypM=1           --BypW=0 --BypWB=0 --Pipe=0 --BrE=0 --BP=1 --Shft=1 --Mul=0 --Div=0 --Opt=1 --Gcc=0",
        :result=>{"total_ticks"=>"1473852"}},
       {"bypassW"=>
         "--BypE=0 --BypM=0           --BypW=1 --BypWB=0 --Pipe=0 --BrE=0 --BP=1 --Shft=1 --Mul=0 --Div=0 --Opt=1 --Gcc=0",
        :result=>{"total_ticks"=>"1563465"}},
       {"bypassWB"=>
         "--BypE=0 --BypM=0           --BypW=0 --BypWB=1 --Pipe=0 --BrE=0 --BP=1 --Shft=1 --Mul=0 --Div=0 --Opt=1 --Gcc=0",
        :result=>{"total_ticks"=>"1751119"}},
       {"bypassAll"=>
         "--BypE=1 --BypM=1           --BypW=1 --BypWB=1 --Pipe=0 --BrE=0 --BP=1 --Shft=1 --Mul=0 --Div=0 --Opt=1 --Gcc=0",
        :result=>{"total_ticks"=>"1247945"}}]},
    {"4-stage"=>
      [{"no bypass"=>
         "--BypE=0 --BypM=0                    --BypWB=0 --Pipe=1 --BrE=1 --BP=1 --Shft=1 --Mul=0 --Div=0 --Opt=1 --Gcc=0",
        :result=>{"total_ticks"=>"1688309"}},
       {"bypassE"=>
         "--BypE=1 --BypM=0                    --BypWB=0 --Pipe=1 --BrE=1 --BP=1 --Shft=1 --Mul=0 --Div=0 --Opt=1 --Gcc=0",
        :result=>{"total_ticks"=>"1424931"}},
       {"bypassM"=>
         "--BypE=0 --BypM=1                    --BypWB=0 --Pipe=1 --BrE=1 --BP=1 --Shft=1 --Mul=0 --Div=0 --Opt=1 --Gcc=0",
        :result=>{"total_ticks"=>"1335033"}},
       {"bypassWB"=>
         "--BypE=0 --BypM=0                    --BypWB=1 --Pipe=1 --BrE=1 --BP=1 --Shft=1 --Mul=0 --Div=0 --Opt=1 --Gcc=0",
        :result=>{"total_ticks"=>"1461405"}},
       {"bypassAll"=>
         "--BypE=1 --BypM=1                    --BypWB=1 --Pipe=1 --BrE=1 --BP=1 --Shft=1 --Mul=0 --Div=0 --Opt=1 --Gcc=0",
        :result=>{"total_ticks"=>"1184633"}}]},
    {"3-stage"=>
      [{"no bypass"=>
         "--BypE=0                             --BypWB=0 --Pipe=2 --BrE=1 --BP=1 --Shft=1 --Mul=0 --Div=0 --Opt=1 --Gcc=0",
        :result=>{"total_ticks"=>"1515458"}},
       {"bypassE"=>
         "--BypE=1                             --BypWB=0 --Pipe=2 --BrE=1 --BP=1 --Shft=1 --Mul=0 --Div=0 --Opt=1 --Gcc=0",
        :result=>{"total_ticks"=>"1281212"}},
       {"bypassWB"=>
         "--BypE=0                             --BypWB=1 --Pipe=2 --BrE=1 --BP=1 --Shft=1 --Mul=0 --Div=0 --Opt=1 --Gcc=0",
        :result=>{"total_ticks"=>"1312515"}},
       {"bypassAll"=>
         "--BypE=1                             --BypWB=1 --Pipe=2 --BrE=1 --BP=1 --Shft=1 --Mul=0 --Div=0 --Opt=1 --Gcc=0",
        :result=>{"total_ticks"=>"1192689"}}]}]}]
